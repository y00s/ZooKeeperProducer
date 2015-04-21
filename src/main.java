import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.proto.WatcherEvent;






public class main {

	public static void main(String[] args) {
		try {
			
			System.out.println("Please enter the text file path: ");
			
			BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
			String filePath = new String(buffer.readLine());
			buffer.close();
			buffer = new BufferedReader(new FileReader(filePath));
			List<String> lines = new ArrayList<String>(); 
			String line = buffer.readLine(); 
			while(line != null){ 
			    lines.add(line);
			    line = buffer.readLine(); 
			    
			}
			
			String address= "127.0.0.1:2181";
			Watcher watcher = null;
			SyncPrimitive.Queue queue = new SyncPrimitive.Queue(address, "/app1");
			
			
			
			for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				try {
					queue.produce(string);
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println(string);
				System.out.println();
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
