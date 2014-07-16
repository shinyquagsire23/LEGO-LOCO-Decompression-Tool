import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.zzl.minegaming.GBAUtils.GBARom;


public class Main
{
	public static byte[] bitmapHeader = new byte[] {(byte)0x42, (byte)0x4D, (byte)0x76, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x36, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x28, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x60, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x70, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x40, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0xC3, (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0xC3, (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
	public static void main(String[] args) throws IOException
	{
		//Testing stuffs
		//args = new String[] { "/offloaded/Program Files (x86)/LEGO Media/Constructive/LEGO LOCO/ART-res-/building/launcher.bmp", "/home/maxamillion/launcher.bmp" };
		System.out.println("LEGO LOCO Decompression Tool v0.1");
		System.out.println("by Shiny Quagsire");
		System.out.println();
		System.out.println("*~* Ported from original ASM for accuracy magics! *~*");
		System.out.println();
		if(args.length < 1)
		{
			System.out.println("Error: Insufficient arguments provided!");
			System.out.println();
			System.out.println("Usage: locodecomp <infile> <outfile>");
			return;
		}
		
		System.out.println("Decompressing contents of " + args[0] + " to file " + args[1] + "...");
		GBARom test = new GBARom(args[0]);
		long length = test.getPointer(0, true);
		long origLength = length;
		byte[] bArr = new byte[(int)length];
		boolean wait = false;
		boolean bitmapMod = false;
		byte waitfor = (byte)0xFF;
		
		if(args[0].toLowerCase().endsWith(".bmp"))
		{
			System.out.print("\nBitmap file detected.\nThis means that you could potentially automatically convert\nthis file to a readable/openable bitmap, at a large risk of failure.\nA .raw file will be saved in the event that it fails.\n\nDo you want to attempt to add a bitmap header?\n(y/n) ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			String answer = "n";
			try
			{
				answer = br.readLine();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			if(answer.toLowerCase().contains("y"))
			{
				int width = 8;
				int height = 8;
				wait = true;
				bitmapMod = true;
				String s = args[0].replace(".bmp", ".dat");
				if(new File(args[0].replace(".bmp", ".dat")).exists())
				{
					String[] lines = readLines(args[0].replace(".bmp", ".dat"));
					int frames = 1;
					for(int i = 0; i < lines.length; i++)
					{
						if(lines[i].toLowerCase().equals("bitmap_occupancy"))
						{
							String size = lines[i+2];
							String xS = size.split(" ")[0];
							String yS = size.split(" ")[1];
							width = Integer.parseInt(xS) * 16;
							height = Integer.parseInt(yS) * 16;
							System.out.println("Found frame size from dat: " + width + " x " + height);
						}
						else if(lines[i].toLowerCase().startsWith("total_number_of_frames"))
						{
							String num = lines[i].split(" ")[1];
							frames = Integer.parseInt(num);
							System.out.println("Found " + frames + " frames");
						}
					}
					
					width *= frames;
				}
				bitmapHeader[0x12] = (byte) (width & 0xFF);
				bitmapHeader[0x13] = (byte) ((width & 0xFF00) >> 8);
				bitmapHeader[0x14] = (byte) ((width & 0xFF0000) >> 16);
				bitmapHeader[0x15] = (byte) ((width & 0xFF000000) >> 24);
				
				bitmapHeader[0x16] = (byte) (height & 0xFF);
				bitmapHeader[0x17] = (byte) ((height & 0xFF00) >> 8);
				bitmapHeader[0x18] = (byte) ((height & 0xFF0000) >> 16);
				bitmapHeader[0x19] = (byte) ((height & 0xFF000000) >> 24);
				
				bArr = new byte[(int)length];
				/*for(int i = 0; i < bitmapHeader.length; i++)
					bArr[i] = bitmapHeader[i];*/
			}
		}
		
		long magic = 0;
		int file_offset = 0x808;
		int mem_ptr = 0;
		long testval = 0;
		boolean doInit = true;
		int count = 0x20;
		
		magic = (test.getPointer(file_offset, true) & 0xFFFFFFFF) >> 1;
		magic = 0xF4;
		
		file_offset += 4;
		mem_ptr--;
		long carry = 0;
		while(length != 0)
		{
			if(doInit)
			{
				testval = test.getPointer(4, true);
				mem_ptr++;
				doInit = false;
			}
			
			if(testval < 0x100)
			{
				if(wait)
				{
					if((byte)(testval & 0xFF) != waitfor)
					{
						length--;
						testval = test.getPointer(4, true);
						continue;
					}
					else
						wait = !wait;
				}
				length--;
				bArr[mem_ptr] = (byte)(testval & 0xFF);
				doInit = true;
				if(mem_ptr % (origLength / 10) == 0)
				{
					System.out.println((mem_ptr / (origLength / 10))*10 + "% complete...");
				}
				continue;
			}
			
			carry = magic & 0x1;
			magic = magic >> 1;
			
			testval = testval + testval + (carry);
			testval = testval << 1;
			
			count--;
			testval = ((test.getPointer((int) testval + 8, true)) & 0xFFFF);
			if(count == 0)
			{
				count = 0x20;
				magic = test.getPointer(file_offset, true);
				file_offset += 4;
			}
		}
		
		if (bitmapMod)
		{
			byte[] newArr = new byte[(int) (origLength + bitmapHeader.length)];
			int spot = 0;
			for (int i = 0; i < bArr.length; i++)
			{
				if (bArr[i] == (byte) 0xFF)
					if (bArr[i + 1] == (byte) 0x00)
						if (bArr[i + 2] == (byte) 0xFF)
							if (bArr[i + 3] == (byte) 0x00)
								if (bArr[i + 4] == (byte) 0x00)
									if (bArr[i + 5] == (byte) 0xFF)
										spot = i;
			}

			for (int i = 0; i < bitmapHeader.length; i++)
				newArr[i] = bitmapHeader[i];
			for (int i = spot; i < origLength; i++)
				newArr[i - spot + bitmapHeader.length] = bArr[i];

			File f = new File(args[1]);
			f.createNewFile();
			FileOutputStream fos = new FileOutputStream(args[1]);
			fos.write(newArr);
			fos.close();
			
			File f1 = new File(args[1]+".raw");
			f1.createNewFile();
			FileOutputStream fos1 = new FileOutputStream(args[1]+".raw");
			fos1.write(bArr);
			fos1.close();
		}
		else
		{
			File f = new File(args[1]);
			f.createNewFile();
			FileOutputStream fos = new FileOutputStream(args[1]);
			fos.write(bArr);
			fos.close();
		}
		
		System.out.println();
		System.out.println("Decompression done!");
	}
	
	public static String[] readLines(String filename) throws IOException
	{
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		List<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null)
		{
			lines.add(line);
		}
		bufferedReader.close();
		return lines.toArray(new String[lines.size()]);
	}
}
