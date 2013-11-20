/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ByteString;

import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.NameSpace;
import eye.Comm.Payload;
import eye.Comm.Request;

/**
 * provides an abstraction of the communication to the remote server.
 * 
 * @author gash
 * 
 */
public class ClientConnection {
	protected static Logger logger = LoggerFactory.getLogger("client");

	private String host;
	private int port;
	private ChannelFuture channel; // do not use directly call connect()!
	private ClientBootstrap bootstrap;
	ClientDecoderPipeline clientPipeline;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker worker;

	protected ClientConnection(String host, int port) {
		this.host = host;
		this.port = port;

		init();
	}

	/**
	 * release all resources
	 */
	public void release() {
		bootstrap.releaseExternalResources();
	}

	public static ClientConnection initConnection(String host, int port) {

		ClientConnection rtn = new ClientConnection(host, port);
		return rtn;
	}

	/**
	 * add an application-level listener to receive messages from the server (as
	 * in replies to requests).
	 * 
	 * @param listener
	 */
	public void addListener(ClientListener listener) {
		try {
			if (clientPipeline != null)
				clientPipeline.addListener(listener);
		} catch (Exception e) {
			logger.error("failed to add listener", e);
		}
	}
	
	// Team Insane starts -- Request builder
	private eye.Comm.Request buildRequest(String filepath, String originator, String toNode, eye.Comm.Header.Routing routeid, byte [] data, String np)
	{
		Document.Builder d = eye.Comm.Document.newBuilder();
		if(null != data && data.length > 0)
		{
			d.setChunkContent(ByteString.copyFrom(data));
			d.setDocSize(data.length);
		}
		d.setChunkId(001);
		d.setTotalChunk(1);
		String[] filename = filepath.split("/");
		d.setDocName(filename[filename.length-1]);
		
		// payload containing data
		String namespace = np;
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setSpace(NameSpace.newBuilder().setName(namespace));
		p.setDoc(d.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator(originator);

		h.setTime(System.currentTimeMillis());
		h.setRoutingId(routeid);
		h.setToNode(toNode);
		r.setHeader(h.build());
		return r.build();
	}
	// Team Insane starts -- to send DOCADD request to the server
	public void docAdd(String filepath, String originator, String toNode, String namespace)
	{
		Path path = Paths.get(filepath);
		
		try {
			byte[] data = Files.readAllBytes(path);
			System.out.println("client toNode-> " + toNode);
			eye.Comm.Request req = buildRequest(filepath, originator, toNode, eye.Comm.Header.Routing.DOCADD, data, namespace);
			// enqueue message
			//System.out.println("Req -> " + req);
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// Team Insane starts -- to send DOCREMOVE request to the server
	public void docRemove(String filename, String originator, String toNode, String namespace)
	{	
		eye.Comm.Request req = buildRequest(filename, originator, toNode, eye.Comm.Header.Routing.DOCREMOVE, null, namespace);
		System.out.println("DocRemove-> " + req.getHeader().toString());
		
		try {
			// enqueue message
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	// Team Insane starts -- to send DOCFIND request to the server
	public void docFind(String filename, String originator, String toNode,String namespace)
	{	
		
		eye.Comm.Request req = buildRequest(filename, originator, toNode, eye.Comm.Header.Routing.DOCFIND, null, namespace);
		System.out.println("DocFind-> " + req.getHeader().toString());
		
	    try {
			// enqueue message
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	// Team Insane starts -- to send DOCQUERY request to the server
	public void docQuery(String filename, String originator, String toNode,String namespace)
	{	
		eye.Comm.Request req = buildRequest(filename, originator, toNode, eye.Comm.Header.Routing.DOCQUERY, null,namespace);

		try {
			// enqueue message
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	//Team Insane starts -- to put forward request message to the outbound queue of server
	// in server to server communication, Server who is forwarding the request will act as client
	public void forwardRequest(Request request){
		try {
			// enqueue message
			outbound.put(request);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	
	public void poke(String toNode,String tag, int num) {
		// data to send
		Finger.Builder f = eye.Comm.Finger.newBuilder();
		f.setTag(tag);
		f.setNumber(num);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setFinger(f.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag("test finger");
		h.setTime(System.currentTimeMillis());
		h.setRoutingId(eye.Comm.Header.Routing.FINGER);
		h.setToNode(toNode);
		r.setHeader(h.build());

		eye.Comm.Request req = r.build();

		try {
			// enqueue message
			//System.out.println("Req -> " + req);
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}

	private void init() {
		// the queue to support client-side surging
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		// Set up the pipeline factory.
		clientPipeline = new ClientDecoderPipeline();
		bootstrap.setPipelineFactory(clientPipeline);

		// start outbound message processor
		worker = new OutboundWorker(this);
		worker.start();
		System.out.println("Client connection queue up!");
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			// System.out.println("---> connecting");
			channel = bootstrap.connect(new InetSocketAddress(host, port));

			// cleanup on lost connection

		}

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess())
			return channel.getChannel();
		else
			throw new RuntimeException("Not able to establish connection to server");
	}

	/**
	 * queues outgoing messages - this provides surge protection if the client
	 * creates large numbers of messages.
	 * 
	 * @author gash
	 * 
	 */
	protected class OutboundWorker extends Thread {
		ClientConnection conn;
		boolean forever = true;

		public OutboundWorker(ClientConnection conn) {
			this.conn = conn;

			if (conn.outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel ch = conn.connect();
			if (ch == null || !ch.isOpen()) {
				ClientConnection.logger.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && conn.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = conn.outbound.take();
					
					if (ch.isWritable()) {
						ClientHandler handler = conn.connect().getPipeline().get(ClientHandler.class);
						
						if (!handler.send(msg))
							conn.outbound.putFirst(msg);

					} else
						conn.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					ClientConnection.logger.error("Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				ClientConnection.logger.info("connection queue closing");
			}
		}
	}
}
