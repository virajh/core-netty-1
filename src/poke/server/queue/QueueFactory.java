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
package poke.server.queue;

import org.jboss.netty.channel.Channel;

import poke.server.routing.ServerHandler.ConnectionClosedListener;

public class QueueFactory {

	public static ChannelQueue getInstance(Channel channel) {
		// if a single queue is needed, this is where we would obtain a
		// handle to it.
		ChannelQueue queue = null;
		//System.out.println("inside queue factory");
		if (channel == null)
			queue = new NoOpQueue();
		else
			queue = new PerChannelQueue(channel);

		// on close remove from queue
		channel.getCloseFuture().addListener(
				new ConnectionClosedListener(queue));

		return queue;
	}

}
