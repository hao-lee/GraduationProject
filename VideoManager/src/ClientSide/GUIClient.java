package ClientSide;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.awt.event.ActionEvent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

// main function
public class GUIClient {
	private String serverIP = "127.0.0.1";
	private int serverPort = 10000;
	private ExecutorService executorService = null;
	private JFrame mainFrame = null;
	private JTable jtableVideoList;
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new GUIClient().createMainInterface();
			}
		});
	}//main
	//create main interface
	public void createMainInterface() {
		// create a ThreadPool
		executorService = Executors.newCachedThreadPool();
		//user-interface
		mainFrame = new JFrame("Main Interface");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setBounds(300, 200, 547, 433);
		
		JPanel mainPanel = new JPanel();
		mainFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 101, 519, 288);
		mainPanel.add(scrollPane);
		DefaultTableModel defaultTableModel = new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"VideoName", "Col2", "Col3"
			}
		);
		jtableVideoList = new JTable(defaultTableModel){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
		}};
		jtableVideoList.setFillsViewportHeight(true);
		scrollPane.setViewportView(jtableVideoList);
		
		JButton btnGetVideoList = new JButton("GetList");
		btnGetVideoList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				defaultTableModel.setRowCount(0);//清空表和原先的内容
				//在界面中会用进度条保证用户不能多次点击get list。所以此处不需要检测线程是否能够开启
				//注意，向列表输出数据是输出到defaultTableModel上，而非JTable
				Callable<Integer> callable = new GUICallable(
						serverIP, serverPort, DefineConstant.GETVIDEOLIST,
						"", defaultTableModel);
				executorService.submit(callable);//不需要收集返回值
			}
		});//addActionListener
		btnGetVideoList.setBounds(12, 37, 107, 25);
		mainPanel.add(btnGetVideoList);
		
		//PlayVideo
		JButton btnPlayVideo = new JButton("PlayVideo");
		btnPlayVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//获取当前被选择的行就不能用tablemodel了，需要用jtable
				int rowIndex = jtableVideoList.getSelectedRow();
				if (rowIndex == -1) {
					JOptionPane.showMessageDialog(null, "No item is selected",
							"Alert", JOptionPane.ERROR_MESSAGE);
					return;}
				String videoName = (String)jtableVideoList.getValueAt(rowIndex, 0);
				//此处需要新弹出一个窗口，用于输出服务端的返回结果，每个线程的输出各自独立
				//这里本身就在EDT里面，所以可以不用invokeAndWait等待子窗口创建，否则会死锁
				//现在在EDT里面已经是线程安全的，直接new即可
				SubThreadFrame subThreadFrame = new SubThreadFrame("Video Playing Status");
				//将子窗口句柄传入子线程，子线程会连接服务器并将数据输出到子窗口
				Callable<Integer> callable = new GUICallable(
					serverIP,serverPort, DefineConstant.PLAYVIDEO, 
					videoName, subThreadFrame.getJTextArea());
				executorService.submit(callable);
			}//actionPerformed
		});//addActionListener
		btnPlayVideo.setBounds(380, 37, 107, 25);
		
		mainPanel.add(btnPlayVideo);
		
		JLabel lblVideolist = new JLabel("Video List");
		lblVideolist.setBounds(12, 74, 83, 15);
		mainPanel.add(lblVideolist);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(8, 0, 228, 21);
		mainPanel.add(menuBar);
		
		JMenu menuSetting = new JMenu("Setting");
		menuBar.add(menuSetting);
		
		JMenuItem mntmServerIp = new JMenuItem("Server IP");
		mntmServerIp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame,"Enter server ip","127.0.0.1");
				serverIP = (str == null?"127.0.0.1":str);
			}
		});
		menuSetting.add(mntmServerIp);
		
		JMenuItem mntmServerPort = new JMenuItem("Server Port");
		mntmServerPort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame,"Enter server port","10000");
				serverPort = (str == null?10000:Integer.valueOf(str));
			}
		});
		menuSetting.add(mntmServerPort);
		
		JMenu menuStop = new JMenu("Stop");
		menuBar.add(menuStop);
		
		JMenuItem mntmStopPlaying = new JMenuItem("Stop playing");
		mntmStopPlaying.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame,"Enter sdp name that you want to stop relay","sdp name");
				SubThreadFrame subThreadFrame = new SubThreadFrame("Stop Relay Status");
				Callable<Integer> callable = new GUICallable(
						serverIP, serverPort, DefineConstant.STOPVTHREAD,
						"", subThreadFrame.getJTextArea());
				executorService.submit(callable);//不需要收集返回值
				//原计划用sdp查出客户端port，让服务器终止和这个port通信的线程，可惜这么做没成功，
				//cancel函数返回的是成功，但是线程并没有取消。现在服务器使用Linux命令直接干掉使用sdpName的进程
				//这样在服务端向sdpName发数据的线程就会因为内建shell进程的终止而从playVideo函数返回，
				//进而正常结束call函数，线程正常结束。
			}
		});
		menuStop.add(mntmStopPlaying);;
		mainFrame.setVisible(true);
	}//create
}//class