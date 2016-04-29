package ClientSide;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.SwingUtilities;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;

// main function
public class Client {
	private String serverIP = "127.0.0.1";
	private int serverPort = 10000;
	private ExecutorService executorService = null;
	private JFrame mainFrame = null;
	JComboBox<String> categoryComboBox = null;
	private int mode = DefineConstant.MODE_LIVE;//播放模式初始值
	//起始序号和步长
	private int videoDisplayStart = 0;//行数从0计
	private int videoDisplayStep = 9;
	
	public static void main(String[] args) {
		Client client = new Client();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				client.createMainInterface();
			}
		});
	}// main

	// 构造函数，同时初始化线程池
	public Client() {
		// create a ThreadPool
		executorService = Executors.newCachedThreadPool();
	}

	// 创建客户端主界面
	private void createMainInterface() {
		int windowWidth = 1000;
		int windowHeight = 500;
		int menuBarHeight = 21;
		int btnPanelHeight = 40;
		int scrollPaneHeight = windowHeight-menuBarHeight-btnPanelHeight;
		// user-interface
		mainFrame = new JFrame("Main Interface");
		mainFrame.setResizable(false);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setBounds(300, 200, windowWidth, windowHeight);

		/* frame的内容面板 */
		JPanel contentPane = new JPanel(null);//绝对布局
		mainFrame.setContentPane(contentPane);
		/*
		 * 内容面板上 可以添加 按钮面板 和 滚动面板（主面板）以及 菜单条
		 * */
		/*设置菜单控件*/
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, windowWidth, menuBarHeight);//大小和位置
		contentPane.add(menuBar);
		
		/*新建按钮面板*/
		JPanel btnPanel = new JPanel(new FlowLayout());
		/* 将按钮面板加到内容面板上 */
		contentPane.add(btnPanel);
		btnPanel.setBounds(0, 0+menuBarHeight, windowWidth, btnPanelHeight);//大小和位置
		
		/*新建滚动面板,滚动面板唯一地做用是为主面板提供滚动条功能*/
		JScrollPane jScrollPane = new JScrollPane();
		/* 将滚动面板加到内容面板上 */
		contentPane.add(jScrollPane);
		/*滚动面板和内容面板（去掉菜单剩下的）一样大即可，后面的主面板要和内容一样大*/
		jScrollPane.setBounds(0, 0+menuBarHeight+btnPanelHeight, windowWidth, scrollPaneHeight);//大小和位置
		
		/*
		 * 新建主面板，并使之具备滚动功能
		 * */
		JPanel mainPanel = new JPanel(new FlowLayout
				(FlowLayout.LEFT, 5, 5));/*主面板上可以添加显示块*/
		jScrollPane.setViewportView(mainPanel);//具备滚动功能
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		/*主面板不用锁定位置，因为有背后的滚动面板来协调，这里秩序根据实际内容的高度设置主面板高度即可*/
		mainPanel.setPreferredSize(new Dimension(windowWidth, 
					DisplayBlock.getTotalHeight()+menuBarHeight+btnPanelHeight));
		/*视频播放模式，直播和点播*/
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton liveRButton = new JRadioButton("直播");
		JRadioButton vodRButton = new JRadioButton("点播");
		buttonGroup.add(liveRButton);
		buttonGroup.add(vodRButton);
		btnPanel.add(vodRButton);
		btnPanel.add(liveRButton);
		liveRButton.setSelected(true);
		/*视频分类下拉列表*/
		categoryComboBox = new JComboBox<>();
		categoryComboBox.setPreferredSize(new Dimension(100, 25));
		btnPanel.add(categoryComboBox);
		/*
		 * 获取分类
		 * */
		getCategoryManually(categoryComboBox);
		/*
		 * 显示块要追加到主面板mainPanel上，
		 * 不要搞成contentPane，contentPane使命到此完成
		 * */
		/*
		 * 设置播放模式
		 * */
		liveRButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				mode = DefineConstant.MODE_LIVE;
			}
		});
		vodRButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				mode = DefineConstant.MODE_VOD;
			}
		});
		/*
		 * 刷新视频列表
		 * */
		JButton btnRefreshVideoList = new JButton("刷新");
		btnRefreshVideoList.setPreferredSize(new Dimension(107, 25));
		btnPanel.add(btnRefreshVideoList);
		btnRefreshVideoList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				/*复位被选择视频块*/
				SelectBlock.resetLastBlock();
				/*获取被选择的列表是哪个*/
				String selectedCategory = (String) categoryComboBox.getSelectedItem();//取被选目录
				ClientCallable callable = new ClientCallable(serverIP, serverPort
						,DefineConstant.ACTION_REFRESHVIDEOLIST,mode,selectedCategory
						,videoDisplayStart,videoDisplayStep);
				callable.setMainPanel(mainPanel);
				executorService.submit(callable);// 不需要收集返回值
				//videoDisplayStart += videoDisplayStep;//递增起点
			}
		});

		/*
		 * 播放视频
		 */
		JButton btnPlayVideo = new JButton("播放视频");
		btnPlayVideo.setPreferredSize(new Dimension(107, 25));
		btnPanel.add(btnPlayVideo);
		btnPlayVideo.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			ClientCallable callable = null;
			if(mode==DefineConstant.MODE_VOD)
				callable = new ClientCallable(serverIP, 
					serverPort,DefineConstant.ACTION_PLAYVOD);
			else//live
				callable = new ClientCallable(serverIP, 
						serverPort,DefineConstant.ACTION_PLAYLIVE);
			executorService.submit(callable);
		}// actionPerformed
		});// addActionListener


		JMenu menuMain = new JMenu("Main");
		menuBar.add(menuMain);

		JMenu menuSetting = new JMenu("Setting");
		menuBar.add(menuSetting);

		/*手动获取分类*/
		JMenuItem mntmGetCategory = new JMenuItem("get");
		menuMain.add(mntmGetCategory);
		mntmGetCategory.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getCategoryManually(categoryComboBox);
			}
		});
		
		/*
		 * 更改要连接的服务器IP地址
		 */
		JMenuItem mntmServerIp = new JMenuItem("Server IP");
		menuSetting.add(mntmServerIp);
		mntmServerIp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame, "Enter server ip", "127.0.0.1");
				serverIP = (str == null ? "127.0.0.1" : str);
			}
		});

		/*
		 * 更改要连接的服务器Port端口
		 */
		JMenuItem mntmServerPort = new JMenuItem("Server Port");
		menuSetting.add(mntmServerPort);
		;
		mntmServerPort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(mainFrame, "Enter server port", "10000");
				serverPort = (str == null ? 10000 : Integer.valueOf(str));
			}
		});

		/*
		 * 显示主窗口
		 */
		mainFrame.setVisible(true);
	}// create

	//刷新分类列表
	private void getCategoryManually(JComboBox<String> categoryComboBox){
		categoryComboBox.removeAllItems();//先清掉之前的项
		ClientCallable callable = new ClientCallable(serverIP, serverPort
				,DefineConstant.ACTION_GETCATEGORY, mode,categoryComboBox);
		executorService.submit(callable);// 不需要收集返回值
	}
	
}// class