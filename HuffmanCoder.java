import java.lang.IllegalArgumentException;
import java.util.Scanner;
import java.util.PriorityQueue;
import java.util.BitSet; //like an array list for bits
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream; //reads bytes from file
import java.io.FileOutputStream; //writes bytes to file

/**
 * This is a java program which Huffman codes files on the byte level.
 * Optimal compression is achieved on files with significant byte repitition, e.g text files.
 * 
 * This was made for an assignment intended to teach about trees. The initial assignment was
 * focused around demonstration hence only required the huffman coding to be output as a
 * String of ones and zeros. With the permission of the teacher I asked of I could take it
 * one step further and output to file. In researching this project I relized examples of 
 * java bit manipulation are somewhat rare on the internet; as such, I've decided to make
 * this little bit of code freely available.
 */

/**
 * Can encode any file with huffman coding and decode files coded with this program
 * The encoding output has .bhc appended to the file name.
 * The interface is a command prompt.
 */
public class HuffmanCoder {

	/**
	 * Runs prompt which allows for specifying command and filepath.
	 * @param args Does not make use of this argument
	 */
	public static void main(String[] args) {
		System.out.print("This program can compress any file with Huffman"+
		                 " Coding.\nThe compressed file is given the same"+
		                 " name with .bhc appended.\n This program can also"+
		                 " decompress .bhc files\n commands:\n"+
		                 "c - compress\nd - decompress");
		Scanner console = new Scanner(System.in);
		while (true) {
			try {
				System.out.println("Input command: ");
				String cmd = console.next();
				System.out.println("Input file path: ");
				String file = console.next();
				if (cmd.equals("c")) encode(file);
				else if (cmd.equals("d")) decode(file);
				else System.out.println("Invalid command");
			}
			catch (Exception e) { System.out.println("bad file or path \n" + e); }
		}
	}

	/**
	 * Takes a sting representation of the filepath and encodes the file
	 * @param file The string filepath to be encode.
	 */
	private static void encode(String file) throws Exception {

		FileInputStream fis = new FileInputStream(new File(file));
		FileOutputStream fos = new FileOutputStream(new File(file + ".bhc"));

		try {
			int[] freq = new int[256];
			long count = 0;
			int inByte = fis.read();
			while (inByte != -1) { //FileInputStream returns a byte worth of file in an int. -1 at end of file.
				count++;
				freq[inByte]++;
				inByte = fis.read();
			}
			HNode tree = buildTree(freq);
			System.out.println("Huffman Tree");
			printTree(tree, 0);
			HNode[] map = new HNode[256];
			buildCodeMap(tree, 0, new BitSet(), map);

			count = Long.reverseBytes(count);//put byte count in file header
			for (int i=0; i<8; i++) {
				fos.write((int)count);
				count = count >>> 8;
			}
			writeTree(fos, tree);

			byte outByte = 0;
			int bitmask = 0x80; //only 8th bit set to true
			HNode node;
			fis = new FileInputStream(new File(file)); //reset the input stream to the start of file
			inByte = fis.read();
			while (inByte != -1) {
				node = map[inByte];
				for (int i=0; i < node.level; i++) {
					if (node.path.get(i)) outByte |= bitmask;
					bitmask = bitmask >>> 1;
					if (bitmask == 0) {
						fos.write(outByte);
						outByte = 0;
						bitmask = 0x80;
					}
				}
				inByte = fis.read();
			}
			if (bitmask != 0x80) fos.write(outByte);
		}
		catch(Exception e) { throw e; }
	}

	/**
	 * Decodes the specified file.
	 * @param file String representation of the path of the file to decode. Must be a file coded by this program.
	 */
	private static void decode(String file) throws Exception {

		FileInputStream fis = new FileInputStream(new File(file));
		FileOutputStream fos = new FileOutputStream(new File(file.substring(0, file.length()-4)));

		try {
			long count = 0;
			for (int i=0; i<8; i++) {
				count = count << 8;
				count += fis.read();
			}

			HNode tree = buildTree(fis);
			printTree(tree, 0);

			HNode node = tree;
			int bitmask = 0x80; //only 8th bit set to true
			int inbyte = fis.read();
			while (count > 0) {
				if (node.left == null) {
					fos.write(node.c);
					node = tree;
					count--;
				}
				else {
					if ((inbyte & bitmask) == 0) node = node.right;
					else node = node.left;
					bitmask = bitmask >>> 1;
					if (bitmask == 0) {
						bitmask = 0x80;
						inbyte = fis.read();
					}
				}
			}
		}
		catch(Exception e) { throw e; }
	}

	/**
	 * Builds a Huffman tree based off the frequency of the chars.
	 * @param freq an int[] size 256 where the frequency of any char is stored in freq[char]
	 */
	private static HNode buildTree(int[] freq) {
		PriorityQueue<HNode> pq = new PriorityQueue<HNode>(512);
		for (char c=0; c<256; c++) {
			if (freq[c] > 0) {
				pq.add(new HNode(c, freq[c]));
			}
		}
		while (pq.size() > 1) {
			HNode n1 = pq.poll();
			HNode n2 = pq.poll();
			pq.add(new HNode(n1.weight + n2.weight, n1, n2)); //smaller on left
		}
		return pq.poll();
	}

	/**
	 * Recursively rebuild the huffman tree from file
	 * @param fis InputStream from file being decoded.
	 */
	private static HNode buildTree(FileInputStream fis) throws Exception {
		try {
			if (fis.read() == 1) return new HNode(buildTree(fis), buildTree(fis));
			else return new HNode((char)fis.read());
		}
		catch (Exception e) { throw e; }
	}

	/**
	 * Writes the huffman tree to file
	 * @param fos The OutputStream of the file being coded.
	 * @param root The root node of the huffman tree
	 */
	private static void writeTree(FileOutputStream fos, HNode root) throws Exception {
		try {
			if (root.left == null) {
				fos.write(0); // leaf nodes represented by 0 byte then char byte.
				fos.write(root.c);
			}
			else {
				fos.write(1); //non-leaf nodes represented by 1 byte
				writeTree(fos, root.left);
				writeTree(fos, root.right);
			}
		}
		catch (Exception e) { throw e; }
	}

	/**
	 * Recursively prints the tree
	 * @param root the root node of tree to print
	 * @param level the level of the root node passed in. Used for indentation.
	 */
	private static void printTree(HNode root, int level) {
		for (int i=0; i<level; i++) System.out.print("  ");
		System.out.print(root.weight+":");
		if (root.left == null) System.out.println(root.c);
		else {
			System.out.println();
			printTree(root.left, level+1);
			printTree(root.right, level+1);
		}
	}

	/**
	 * Builds a hash map from chars to HNodes
	 * @param root The root of the tree to be mapped
	 * @param level The depth of the root node.
	 * @param path The path to the root node. Right is zero, left is one.
	 * @param map An HNode array size 256. Each node will be mapped to the index of it's char.
	 */
	private static void buildCodeMap(HNode root, int level, BitSet path, HNode[] map) {
		if (root.left == null) {
			root.level = level;
			root.path = path;
			map[root.c] = root;
		}
		else { // right is zero
			buildCodeMap(root.right, level+1, (BitSet)path.clone(), map);
			path.set(level); // left is one
			buildCodeMap(root.left, level+1, (BitSet)path.clone(), map);
		}
	}

	/**
	 * Node of char Huffman tree.
	 */
	static class HNode implements Comparable<HNode> {
		char c; /**The char for leaf nodes*/
		int weight; /**the weight used for the construction of the huffman tree*/
		int level; /**level of node b/c BitSet doesn't store size*/
		BitSet path; /**left is one, right is zero*/
		HNode left; /**The HNode to the left*/
		HNode right; /**The HNode to the right*/

		/**
		 * constructor for just adding a char, used when building from file.
		 * @param c the char
		 */
		public HNode(char c) { this.c = c; }
		/**
		 * Constructor for adding the char and it's weight, for building tree from char frequency.
		 * @param c the char.
		 * @param weight the frequency of the char in the file to be coded.
		 */
		public HNode(char c, int weight) {
			this.c = c;
			this.weight = weight;
		}
		/**
		 * Constructor for non-leaf nodes without weight, used when building from file.
		 * @param left The left branch HNode.
		 * @param right The right branch HNode.
		 */
		public HNode(HNode left, HNode right) {
			this.left = left;
			this.right = right;
		}
		/**
		 * Constructor for non-leaf nodes with weight, used when building tree from char from char frequency.
		 * @param weight The sum of the weights of the right and left HNodes
		 * @param left The left HNode
		 * @param right The right HNode
		 */
		public HNode(int weight, HNode left, HNode right) {
			this.weight = weight;
			this.left = left;
			this.right = right;
		}

		/**
		 * Compares HNodes based on weight
		 * @param obj The HNode we are comparing to the current node.
		 * @return 1 if the current object weighs more. -1 if it weighs less. 0 if the weights are the same.
		 */
		public int compareTo(HNode obj) {
			if (weight > obj.weight) return 1;
			if (weight < obj.weight) return -1;
			return 0;
		}
	}
}
