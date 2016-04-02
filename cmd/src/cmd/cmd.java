package cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class cmd
{
	public static void main(String[] args){
		InputStream in = null;
		String[] cmd = {"sh","-c","ffmpeg -re -i ~/transformers2.mp4 -f rtsp rtsp://127.0.0.1/test.sdp"};
		String[] cmd2 = {"sh","-c","ping -c 20 127.0.0.1"};
		try{
			Process pc = null;//Runtime.getRuntime().exec(cmd2);
			ProcessBuilder pb = null;
			pb = new ProcessBuilder(cmd2);
			pb.redirectErrorStream(true);
			pc = pb.start();
			in = pc.getInputStream();
			BufferedReader bfr_in = new BufferedReader(new InputStreamReader(in));
			String tmp_in = null;
			while((tmp_in = bfr_in.readLine()) != null){
				System.out.println(tmp_in);
			}
			pc.waitFor();
			pc.destroy();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}