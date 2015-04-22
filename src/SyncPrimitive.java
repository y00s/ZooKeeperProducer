import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * Primitive class that synchronize with ZooKeeper implemented for both Producer
 * and Consumer
 * 
 * mostly copied from the ZooKeeper example, some changes where made to support
 * string queue
 * 
 * Yosef
 */
public class SyncPrimitive implements Watcher {

	// Global variables:
	static ZooKeeper zk = null;
	static Integer mutex;
	String root;

	/**
	 *
	 * Default constructor: if a connection does not exist start a new one and
	 * set mutex to -1
	 * 
	 * Yosef
	 */
	SyncPrimitive(String address) {
		if (zk == null) {
			try {
				System.out.println("Starting ZK:");
				zk = new ZooKeeper(address, 3000, this);
				mutex = new Integer(-1);
				System.out.println("Finished starting ZK: " + zk);
			} catch (IOException e) {
				System.out.println(e.toString());
				zk = null;
			}
		}
	}

	/**
	 *
	 * Implementation of Watcher so the process get synchronization with respect
	 * to the mutex
	 * 
	 * Yosef
	 */
	synchronized public void process(WatchedEvent event) {
		synchronized (mutex) {
			// System.out.println("Process: " + event.getType());
			mutex.notify();
		}
	}

	/**
	 * Producer-Consumer queue class this class create a queue in the ZooKeeper
	 * as a Znode such that the object, in this specific case Strings, can be
	 * put in a queue and consumed one by one with out Overlapping one another
	 * 
	 * Yosef
	 */
	static public class Queue extends SyncPrimitive {

		/**
		 * Constructor of producer-consumer queue
		 *
		 * Create a queue if it does not exist already
		 * 
		 * @param address
		 * @param name
		 */
		Queue(String address, String name) {
			super(address);
			this.root = name;
			// Create ZK node name
			if (zk != null) {
				try {
					Stat s = zk.exists(root, false);
					if (s == null) {
						zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
								CreateMode.PERSISTENT);
					}
				} catch (KeeperException e) {
					System.out
							.println("Keeper exception when instantiating queue: "
									+ e.toString());
				} catch (InterruptedException e) {
					System.out.println("Interrupted exception");
				}
			}
		}

		/**
		 * Add element to the queue. get a String and encode it into a Byte
		 * buffer then put it as a node son of the queue node or in short a
		 * "element"
		 *
		 * @param string
		 * @return
		 */

		boolean produce(String string) throws KeeperException,
				InterruptedException {
			ByteBuffer b = null;
			byte[] value = null;
			// Encode String
			try {
				Charset charset = Charset.forName("UTF-8");
				CharsetEncoder encoder = charset.newEncoder();
				b = encoder.encode(CharBuffer.wrap(string));
				value = new byte[b.limit()];
				b.get(value);

			} catch (CharacterCodingException ex) {

				System.out
						.println("produce exception when codeing string to bytes: "
								+ ex.toString());
				return false;
			}

			String result = zk.create(root + "/element", value,
					Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
			System.out.println("Znode created: " + result);

			return true;
		}

		/**
		 * Remove first element from the queue.
		 *
		 * get the byte array from the queue and decode it into a String then
		 * return it to the consumer
		 * 
		 * @param string
		 * @return
		 *
		 * @return
		 * @throws KeeperException
		 * @throws InterruptedException
		 */
		String consume() throws KeeperException, InterruptedException {
			String retvalue = "";
			Stat stat = null;

			// Get the first element available
			while (true) {
				synchronized (mutex) {
					List<String> list = zk.getChildren(root, true);
					if (list.size() == 0) {
						System.out.println("Going to wait");
						mutex.wait();
					} else {
						Integer min = new Integer(list.get(0).substring(7));
						String nodeName = list.get(0);
						for (String s : list) {
							Integer tempValue = new Integer(s.substring(7));
							if (tempValue < min) {
								min = tempValue;
								nodeName = s;

							}

						}
						System.out.println("Node picked: " + root + "/"
								+ nodeName);
						byte[] b = zk.getData(root + "/" + nodeName, false,
								stat);
						zk.delete(root + "/" + nodeName, 0);

						// decode the string
						ByteBuffer buffer = ByteBuffer.wrap(b);
						Charset charset = Charset.forName("UTF-8");
						CharsetDecoder decoder = charset.newDecoder();
						try {
							retvalue = decoder.decode(buffer).toString();
						} catch (CharacterCodingException e) {
							System.out
									.println("Consume exception when decodeing bytes to string: "
											+ e.toString());
							return null;

						}

						return retvalue;
					}
				}
			}
		}
	}
}
