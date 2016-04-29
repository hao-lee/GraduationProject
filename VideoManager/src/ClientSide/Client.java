package ClientSide;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.SwingUtilities;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
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
	JList<String> categoryList = null;
	DefaultListModel<String> categoryListModel = null;
	private int mode = DefineConstant.MODE_LIVE;//播放模式初始值
	//起始序号和步长
	private int videoDisplayStart = 0;//行数从0计
	private int videoDisplayStep = 9;
	private static boolean canPreviousPage = true;
	private static boolean canNextPage = true;
	
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
		int windowWidth = 1000;//宽度
		int windowHeight = 600;//高度
		int mainPanelHeight = DisplayBlock.getTotalHeight()+4*5;//行间距为5
		// user-interface
		mainFrame = new JFrame("Main Interface");
		mainFrame.setResizable(false);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setBounds(100, 0, windowWidth, windowHeight);

		/* frame的内容面板 */
		JPanel contentPane = new JPanel(null);//绝对布局
		mainFrame.setContentPane(contentPane);
		/*
		 * 内容面板上 可以添加 按钮面板 和 滚动面板（主面板）以及 菜单条
		 * */
		/*设置菜单控件*/
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, windowWidth, 21);//大小和位置
		contentPane.add(menuBar);
		
		/*新建按钮上面板，存放分类列表、刷新按钮*/
		JPanel upPanel = new JPanel(new FlowLayout());//绝对布局
		/* 将按钮上面板加到内容面板上 */
		contentPane.add(upPanel);
		upPanel.setBounds(0, 21, windowWidth, 36);//大小和位置
		
		/*新建按钮下面板，存放播放按钮、上下翻页按钮*/
		JPanel downPanel = new JPanel(new FlowLayout());
		downPanel.setBounds(3, 526, windowWidth, 36);//大小和位置
		contentPane.add(downPanel);
		
		/*新建滚动面板,滚动面板唯一地做用是为主面板提供滚动条功能*/
		JScrollPane jScrollPane = new JScrollPane();
		/* 将滚动面板加到内容面板上 */
		contentPane.add(jScrollPane);
		/*滚动面板和内容面板（去掉菜单剩下的）一样高即可，宽度减去3为了让滚动条宽度更好看*/
		jScrollPane.setBounds(0, 61, windowWidth, 462);//大小和位置
		
		/*
		 * 新建主面板，并使之具备滚动功能
		 * */
		JPanel mainPanel = new JPanel(new FlowLayout
				(FlowLayout.LEFT, 5, 5));/*主面板上可以添加显示块，注意每行有5像素间隔*/
		jScrollPane.setViewportView(mainPanel);//具备滚动功能
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		/*主面板不用锁定位置，因为有背后的滚动面板来协调，这里秩序根据实际内容的高度设置主面板高度即可*/
		mainPanel.setPreferredSize(new Dimension(windowWidth,mainPanelHeight));
		/*视频播放模式，直播和点播*/
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton liveRButton = new JRadioButton("直播");
		JRadioButton vodRButton = new JRadioButton("点播");
		buttonGroup.add(liveRButton);
		buttonGroup.add(vodRButton);
		upPanel.add(vodRButton);
		upPanel.add(liveRButton);
		liveRButton.setSelected(true);
		
		/*视频分类列表*/
		categoryListModel = new DefaultListModel<>();
		categoryList = new JList<>(categoryListModel);
		categoryList.setLayoutOrientation(JList.HORIZONTAL_WRAP);//水平显示，可以折行
		categoryList.setVisibleRowCount(1);//最多折两行
		categoryList.setPreferredSize(new Dimension(300, 20));
		upPanel.add(categoryList);
		
		/*
		 * 获取分类
		 * */
		getCategoryManually(categoryList);
		/*
		 * 显示块要追加到主面板mainPanel上，
		 * 不要搞成contentPane，contentPane使命到此完成
		 * */
		/*
		 * 单选按钮，设置播放模式
		 * */
		/*注意，不管点击哪个按钮，两个按钮的itemStateChanged事件都会触发，所以只需监听一个即可*/
		liveRButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.DESELECTED)//liveRButton被取消，切换到点播
					mode = DefineConstant.MODE_VOD;
				else								//liveRButton被选择，切换成直播
					mode = DefineConstant.MODE_LIVE;
				getCategoryManually(categoryList);//重新获取分类
			}
		});
		
		/*
		 * 刷新视频列表
		 * */
		JButton btnRefreshVideoList = new JButton("刷新视频列表");
		btnRefreshVideoList.setPreferredSize(new Dimension(120, 25));
		upPanel.add(btnRefreshVideoList);
		btnRefreshVideoList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个*/
				String selectedCategory = (String) categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				/*复位记忆被选择视频块全局变量*/
				SelectBlock.resetLastBlock();
				ClientCallable callable = new ClientCallable(serverIP, serverPort
						,DefineConstant.ACTION_REFRESHVIDEOLIST,mode,selectedCategory
						,videoDisplayStart,videoDisplayStep);
				callable.setMainPanel(mainPanel);
				executorService.submit(callable);// 不需要收集返回值
			}
		});

		/*
		 * 上一页
		 * */
		JButton btnPrevious = new JButton("上一页");
		btnPrevious.setPreferredSize(new Dimension(107, 25));
		downPanel.add(btnPrevious);
		btnPrevious.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个*/
				String selectedCategory = (String) categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				if(canPreviousPage)
					;
				else {
					JOptionPane.showMessageDialog(null, "已经是第一页"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				/*复位记忆被选择视频块全局变量*/
				SelectBlock.resetLastBlock();
				videoDisplayStart -= videoDisplayStep;//起点减少
				if(videoDisplayStart < 0)//如果翻过头了，直接赋0
					videoDisplayStart = 0;
				ClientCallable callable = new ClientCallable(serverIP, serverPort
						,DefineConstant.ACTION_REFRESHVIDEOLIST,mode,selectedCategory
						,videoDisplayStart,videoDisplayStep);
				callable.setMainPanel(mainPanel);
				executorService.submit(callable);// 不需要收集返回值
			}
		});
		
		/*
		 * 下一页
		 * */
		JButton btnNext = new JButton("下一页");
		btnNext.setPreferredSize(new Dimension(107, 25));
		downPanel.add(btnNext);
		btnNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个*/
				String selectedCategory = (String) categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				if(canNextPage)
					;
				else {
					JOptionPane.showMessageDialog(null, "已经是最后一页"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				/*复位记忆被选择视频块全局变量*/
				SelectBlock.resetLastBlock();
				videoDisplayStart += videoDisplayStep;//起点增加
				ClientCallable callable = new ClientCallable(serverIP, serverPort
						,DefineConstant.ACTION_REFRESHVIDEOLIST,mode,selectedCategory
						,videoDisplayStart,videoDisplayStep);
				callable.setMainPanel(mainPanel);
				executorService.submit(callable);// 不需要收集返回值
			}
		});
		
		/*
		 * 播放视频
		 */
		JButton btnPlayVideo = new JButton("播放视频");
		btnPlayVideo.setPreferredSize(new Dimension(107, 25));
		downPanel.add(btnPlayVideo);
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
		JMenuItem mntmGetCategory = new JMenuItem("手动获取列表");
		menuMain.add(mntmGetCategory);
		mntmGetCategory.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getCategoryManually(categoryList);
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
	private void getCategoryManually(JList<String> categoryList){
		categoryListModel.removeAllElements();
		categoryList.revalidate();
		categoryList.repaint();
		ClientCallable callable = new ClientCallable(serverIP, serverPort
				,DefineConstant.ACTION_GETCATEGORY, mode,categoryListModel);
		executorService.submit(callable);// 不需要收集返回值
	}

	//禁止上翻
	public static void ProhibitPreviousPage(){
		canPreviousPage = false;
	}
	//禁止下翻
	public static void ProhibitNextPage(){
		canNextPage = false;
	}
	//开启上翻
	public static void AllowPreviousPage(){
		canPreviousPage = true;
	}
	//开启下翻
	public static void AllowNextPage(){
		canNextPage = true;
	}

}// class