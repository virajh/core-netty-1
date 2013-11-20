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
package poke.demo;

import poke.client.ClientConnection;
import poke.client.ClientListener;
import poke.client.ClientPrintListener;

public class Jab {
	private String tag;
	private int count;

	public Jab(String tag) {
		this.tag = tag;
	}

	public void run(String arg) {
		// Team Insane: Client code
		ClientConnection cc = ClientConnection.initConnection("192.168.0.5", 5580);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);

		
		if(arg.contentEquals("2"))
			cc.docFind("README.txt", "JabClient", "one", "Team9");
		else if(arg.contentEquals("3"))
			cc.docRemove("README.txt", "JabClient", "one", "Team9");
		else
		{
			cc.docAdd("/home/ranjan/hello.txt", "JabClient", "eight", "Team9");
			//cc.docAdd("/home/ranjan/hello.txt", "JabClient", "one", "Team9");
		}

		//}
	}

	public static void main(String[] args) {
		try {
			Jab jab = new Jab("jab");
			System.out.println("\nJab.run called");
			
			String param="1";
			
			jab.run(param);

			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			Thread.sleep(9000);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
