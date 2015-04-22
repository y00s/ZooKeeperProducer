import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.zookeeper.KeeperException;

public class main {

	public static void main(String[] args) {
		try {

			// Get the file with the text to process and put the lines into a
			// list
			System.out.println("Please enter the text file path: ");
			BufferedReader buffer = new BufferedReader(new InputStreamReader(
					System.in));
			String filePath = new String(buffer.readLine());
			buffer.close();
			buffer = new BufferedReader(new FileReader(filePath));
			List<String> lines = new ArrayList<String>();
			String line = buffer.readLine();
			while (line != null) {
				lines.add(line);
				line = buffer.readLine();

			}

			// create or get the TEXTS queue in ZooKeeper
			// the address is for running locally 
			String address = "127.0.0.1:2181";
			SyncPrimitive.Queue queue = new SyncPrimitive.Queue(address,
					"/TEXTS");

			// For each line create an element with the line-string
			for (Iterator<String> iterator = lines.iterator(); iterator
					.hasNext();) {
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
