package ClientSide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.jar.Attributes.Name;

public class UIDesign {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIDesign window = new UIDesign();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public UIDesign() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 700, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel  mainPane=new  JPanel();//主面板，用来添加视频显示块的
		JScrollPane scrollPane = new  JScrollPane(mainPane);//将主面板附到滚动面板上
		frame.setContentPane(scrollPane);//将滚动面板作为窗口内容面板
		scrollPane.setPreferredSize(new Dimension(700, 500));//设置内容面板和窗口一样大
		mainPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));//设置主面板为流式布局
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		/*
		 * 添加缩略图
		 * */
		String imagePath = "001.jpg";
		HashMap<String, String> infoMap = new HashMap<>();
		infoMap.put("VideoName", "dog");
		infoMap.put("Duration", "00:05:23");
		infoMap.put("Resolution","1920x1080");
		for(int i = 1; i<=5;i++){
			try {
				BufferedImage bufferedImage = ImageIO.read(new FileInputStream(imagePath));
				//新建显示块
				DisplayBlock displayBlock = new DisplayBlock(bufferedImage,infoMap);
				mainPane.add(displayBlock);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}//for
		mainPane.setPreferredSize(new Dimension(700, DisplayBlock.getTotalHeight()));;
	}

}

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
	private int tnWidth = 300, tnHeight = 200;//缩略图面板大小，也是图片大小
	private int blockWidth = -1,blockHeight  = -1;//DisplayBlock显示块大小
	private int padding = 10;//显示块面板与缩略图面板之间的内边距
	private int infoHeight = 50;//信息显示区域的高度
	private static int totalHeight = 0;
	public static int getTotalHeight() {
		return totalHeight;
	}
	//HashMap<>()存储视频时长等信息
	public DisplayBlock(BufferedImage bufferedImage, HashMap<String, String> infoMap) {
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
				DisplayBlock thisDisplayBlock = (DisplayBlock)e.getSource();
				SelectBlock.changeSelectionBlock(thisDisplayBlock);//选块切换
			}
		});
		//在显示块上添加缩略图面板
		Thumbnail thumbnailPanel = new Thumbnail(tnWidth,tnHeight,bufferedImage);
		thumbnailPanel.repaint();//在显示块上画出缩略图
		this.add(thumbnailPanel);//将缩略图加到显示块上去
		thumbnailPanel.setBounds(padding,padding, tnWidth,tnHeight);//定位缩略图面板的位置,同时设置大小
		//在显示块上添加文本信息面板
		Info info = new Info(infoMap);
		this.add(info);
		info.setBounds(padding, tnHeight+2*padding, tnWidth,infoHeight);//定位信息面板的位置,同时设置大小
		//每新建一个显示块，就把总高度累加
		totalHeight += blockHeight;
	}
}

/*
 * 缩略图面板，不用设置布局，图片直接画上去
 * */
class Thumbnail extends JPanel{
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

class Info extends JPanel{
	public Info(HashMap<String, String> infoMap) {
		this.setBackground(new Color(255, 204, 204));
		this.setLayout(new GridLayout(3, 1, 0, 2));//行数 列数 水平间隔 竖直间隔
		JLabel jLabelVideoName = new JLabel("VideoName:"+infoMap.get("VideoName"));
		JLabel jLabelDuration = new JLabel("Duration:"+infoMap.get("Duration"));
		JLabel jLabelResolution = new JLabel("Resolution:"+infoMap.get("Resolution"));
		this.add(jLabelVideoName);
		this.add(jLabelDuration);
		this.add(jLabelResolution);
	}
}

final class SelectBlock{
	private static DisplayBlock lastBlock = null;//显示块对象的引用
	private static Color noSelectionColor = new Color(199, 237, 204);//未选择时颜色
	private static Color selectionColor = new Color(51, 85, 254);//被选择时颜色

	public static void changeSelectionBlock(DisplayBlock displayBlock) {
		if(lastBlock != null)//第一次清空时，它没有上一块
			lastBlock.setBackground(noSelectionColor);//清空上一块颜色
		displayBlock.setBackground(selectionColor);//设置本块颜色
		lastBlock = displayBlock;//本块变为上一块
	}
	public static Color getNoSelectionColor() {
		return noSelectionColor;
	}
}