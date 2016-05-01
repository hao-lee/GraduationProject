package ClientSide;

import CommonPackage.*;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

//public class BlockPanel {
//
//	private JFrame frame;
//
//	/**
//	 * Launch the application.
//	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					BlockPanel window = new BlockPanel();
//					window.frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}
//
//	/**
//	 * Create the application.
//	 */
//	public BlockPanel() {
//		initialize();
//	}
//
//	/**
//	 * Initialize the contents of the frame.
//	 */
//	private void initialize() {
//		frame = new JFrame();
//		frame.setBounds(100, 100, 700, 500);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		JPanel  mainPane=new  JPanel();//主面板，用来添加视频显示块的
//		JScrollPane scrollPane = new  JScrollPane(mainPane);//将主面板附到滚动面板上
//		frame.setContentPane(scrollPane);//将滚动面板作为窗口内容面板
//		scrollPane.setPreferredSize(new Dimension(700, 500));//设置内容面板和窗口一样大
//		mainPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));//设置主面板为流式布局
//		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//		
//		/*
//		 * 添加缩略图
//		 * */
//		String imagePath = "001.jpg";
//		VideoInfo videoInfo = new VideoInfo("dog", "00:05:23", "1920x1080", "游戏", 
//					"vod/games/Ghost Recon.mp4");
//		for(int i = 1; i<=5;i++){
//			try {
//				BufferedImage bufferedImage = ImageIO.read(new FileInputStream("/home/mirage/GraduationProject/VideoManager/tmp.jpg"));
//				//新建显示块
//				videoInfo.setBufferedImage(bufferedImage);
//				DisplayBlock displayBlock = new DisplayBlock(videoInfo);
//				mainPane.add(displayBlock);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}//for
//		mainPane.setPreferredSize(new Dimension(700, DisplayBlock.getTotalHeight()));;
//	}
//
//}

/*

               + <-------+width+2*padding+------> +----+
               |                                       |
          +-+------------------------------------------+
            ^  |               padding                 |
            |  |  +--------------------------------+   |
            |  |p |                                |   |
            +  |a |                                |   |
height+        |d |                                |   |
2*padding+     |d |                                |   |
infoAreaHeight |i |                                |   |
               |n |                                |   |
            +  |g |                                |   |
            |  |  |                                |   |
            |  |  +--------------------------------+   |
            v  |                                       |
               |                                       |
          +----+---------------------------------------+


 * 视频显示块面板
 * */
class DisplayBlock extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int tnWidth = 300, tnHeight = 200;//缩略图面板大小，也是图片大小
	private int blockWidth = -1,blockHeight  = -1;//DisplayBlock显示块大小
	private int padding = 10;//显示块面板与缩略图面板之间的内边距
	private int infoHeight = 50;//信息显示区域的高度
	private VideoInfo videoInfo = null;
	//计算主面板应该多高,主需要一个静态的即可，该函数只在添加主面板时使用一次
	public static int getTotalHeight() {
		//blockHeight = tnHeight+3*padding+infoHeight;
		return 3*(200+3*10+50);//共显示3行，每行3个
	}
	//返回当前视频显示块对象内的videoInfo对象
	public VideoInfo getVideoInfo() {
		return videoInfo;
	}
	//HashMap<>()存储视频时长等信息
	public DisplayBlock(VideoInfo videoInfo) {
		
		this.videoInfo = videoInfo;
		//计算DisplayBlock显示块大小
		blockWidth = tnWidth+2*padding;
		blockHeight = tnHeight+3*padding+infoHeight;
		this.setPreferredSize(new Dimension(blockWidth,blockHeight));//显示块面板大小，上下左右各比缩略图面板大padding
		this.setBackground(SelectBlock.getNoSelectionColor());//显示块面板初始背景
		this.setLayout(null);//绝对布局
		//鼠标点击时呈现出该显示块被选择的效果
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				System.out.println("CLick");
				//从事件里反向获取当前组件的索引
				DisplayBlock thisDisplayBlock = (DisplayBlock)e.getSource();
				SelectBlock.changeSelectionBlock(thisDisplayBlock);//选块切换
			}
		});
		BufferedImage bufferedImage = videoInfo.getBufferedImage();
		//在显示块上添加缩略图面板
		Thumbnail thumbnailPanel = new Thumbnail(tnWidth,tnHeight,bufferedImage);
		thumbnailPanel.repaint();//在显示块上画出缩略图
		this.add(thumbnailPanel);//将缩略图加到显示块上去
		thumbnailPanel.setBounds(padding,padding, tnWidth,tnHeight);//定位缩略图面板的位置,同时设置大小
		//在显示块上添加文本信息面板
		Info info = new Info(
				videoInfo.getVideoName(),
				videoInfo.getDuration(),
				videoInfo.getResolution());
		this.add(info);
		info.setBounds(padding, tnHeight+2*padding, tnWidth,infoHeight);//定位信息面板的位置,同时设置大小
	}
}

/*
 * 缩略图面板，不用设置布局，图片直接画上去
 * */
class Thumbnail extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Image scaledImage = null;
	//构造函数
	public Thumbnail(int tnWidth , int tnHeight, BufferedImage bufferedImage) {
		this.scaledImage = bufferedImage.getScaledInstance(tnWidth, tnHeight, Image.SCALE_DEFAULT);
	}
	
	/*
	 * 不能用getGraphics获取Graphics对象再调用drawImage方法画图，这样的话getGraphics返回的一定是null
	 * 应该重写paintCommonent方法，然后在需要画图片的时候调用repaint方法即可
	 * 参考：http://stackoverflow.com/questions/23717634/nullpointerexception-when-calling-graphics-drawimage/23717710#23717710
	 * http://stackoverflow.com/questions/15986677/drawing-an-object-using-getgraphics-without-extending-jframe/15991175#15991175
	 * */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(scaledImage, 0, 0, this);
	}
}

/*视频信息显示面板，包括视频名、时长、分辨率*/
class Info extends JPanel{

	private static final long serialVersionUID = -8194540666523790574L;
	public Info(String videoName,String duration,String resolution) {
		this.setBackground(new Color(255, 204, 204));
		this.setLayout(new GridLayout(3, 1, 0, 2));//行数 列数 水平间隔 竖直间隔
		JLabel jLabelVideoName = new JLabel("VideoName:"+videoName);
		JLabel jLabelDuration = new JLabel("Duration:"+duration);
		JLabel jLabelResolution = new JLabel("Resolution:"+resolution);
		this.add(jLabelVideoName);
		this.add(jLabelDuration);
		this.add(jLabelResolution);
	}
}


/*静态变量和全局方法，记录当前被选中的块*/
class SelectBlock{
	private static DisplayBlock selectedBlock = null;//被选择视频块对象的引用
	private static Color noSelectionColor = new Color(199, 237, 204);//未选择时颜色
	private static Color selectionColor = new Color(51, 85, 254);//被选择时颜色
	/*获得被选择的视频块*/
	public static DisplayBlock getSelectedBlock() {
		return selectedBlock;
	}
	public static void changeSelectionBlock(DisplayBlock displayBlock) {
		if(selectedBlock != null)//第一次清空时，它没有上一块
			selectedBlock.setBackground(noSelectionColor);//清空上一块颜色
		displayBlock.setBackground(selectionColor);//设置本块颜色
		selectedBlock = displayBlock;//本块变为上一块
	}
	public static Color getNoSelectionColor() {
		return noSelectionColor;
	}
	/**
	 * 该函数将lastBlock置为Null
	 */
	public static void resetSelectedBlock() {
		selectedBlock = null;
	}
}