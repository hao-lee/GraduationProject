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
import java.awt.event.ActionEvent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

// main function
public class Client {
	private String serverIP = "127.0.0.1";
	private int serverPort = 10000;
	private ExecutorService executorService = null;
	private JFrame mainFrame = null;
	private JTable jtableVideoList;
	
	public static void main(String[] args) {
		Client client = new Client();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				client.createMainInterface();
			}
		});
	}//main
	
	//构造函数，同时初始化线程池
	public Client() {
		// create a ThreadPool
		executorService = Executors.newCachedThreadPool();
	}
	//创建客户端主界面
	public void createMainInterface() {
		//user-interface
		mainFrame = new JFrame("Main Interface");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setBounds(300, 200, 547, 433);
		
		JPanel mainPanel = new JPanel();
		mainFrame.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(null);
		
		/*
		 * 表格上面的名字标签
		 * */
		JLabel lblVideolist = new JLabel("Video List");
		lblVideolist.setBounds(12, 74, 83, 15);
		mainPanel.add(lblVideolist);
		/*
		 * 新建滚动面板
		 * */
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 101, 519, 288);
		mainPanel.add(scrollPane);
		/*
		 * 新建表格模型
		 * */
		DefaultTableModel defaultTableModel = new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"VideoName", "Col2", "Col3"
			}
		);
		/*
		 * 以刚才的表格模型为基准新建表格控件，并将之添加到滚动面板上
		 * */
		jtableVideoList = new JTable(defaultTableModel){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
		}};
		jtableVideoList.setFillsViewportHeight(true);
		scrollPane.setViewportView(jtableVideoList);
		
		/*
		 * 获取视频列表
		 * */
		JButton btnGetVideoList = new JButton("GetList");
		btnGetVideoList.setBounds(12, 37, 107, 25);
		mainPanel.add(btnGetVideoList);
		btnGetVideoList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				defaultTableModel.setRowCount(0);//清空表中原先的内容
				//注意，向列表输出数据是输出到defaultTableModel上，而非JTable
				Callable<Integer> callable = new GUICallable(
						serverIP, serverPort, DefineConstant.GETVIDEOLIST,
						"", defaultTableModel);
				executorService.submit(callable);//不需要收集返回值
			}
		});//addActionListener
		
		/*
		 * 播放视频
		 * */
		JButton btnPlayVideo = new JButton("PlayVideo");
		btnPlayVideo.setBounds(380, 37, 107, 25);
		mainPanel.add(btnPlayVideo);
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
		
		/*
		 * 设置菜单控件
		 * */
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(8, 0, 228, 21);
		mainPanel.add(menuBar);
		
		JMenu menuMain = new JMenu("Main");
		menuBar.add(menuMain);
		
		JMenu menuSetting = new JMenu("Setting");
		menuBar.add(menuSetting);
		/*
		 * 获取当前服务器端视频的播放状态
		 * */
		JMenuItem mntmGetStatus = new JMenuItem("Get Status");
		menuMain.add(mntmGetStatus);
		mntmGetStatus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SubThreadFrame subThreadFrame = new SubThreadFrame("Videos that are Playing");
				Callable<Integer> callable = new GUICallable(
						serverIP, serverPort, DefineConstant.GETVIDEOSTATUS,
						"", subThreadFrame.getJTextArea());
				executorService.submit(callable);
			}
		});
		
		/*
		 * 停止播放某个视频
		 * */
		JMenuItem mntmStopPlaying = new JMenuItem("Stop Playing");
		menuMain.add(mntmStopPlaying);
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
		
		/*
		 * 更改要连接的服务器IP地址
		 * */
		JMenuItem mntmServerIp = new JMenuItem("Server IP");
		menuSetting.add(mntmServerIp);
		mntmServerIp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame,"Enter server ip","127.0.0.1");
				serverIP = (str == null?"127.0.0.1":str);
			}
		});
		
		/*
		 * 更改要连接的服务器Port端口
		 * */
		JMenuItem mntmServerPort = new JMenuItem("Server Port");
		menuSetting.add(mntmServerPort);;
		mntmServerPort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame,"Enter server port","10000");
				serverPort = (str == null?10000:Integer.valueOf(str));
			}
		});
		
		/*
		 * 显示主窗口
		 * */
		mainFrame.setVisible(true);
	}//create
}//class