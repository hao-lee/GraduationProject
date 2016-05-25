package me.haolee.gp.clientside;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import me.haolee.gp.common.*;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

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
class VideoPanel extends JPanel{

	private static final long serialVersionUID = 5857923303664996266L;
	/**
	 * 
	 */
	private int tnWidth = 320, tnHeight = 240;//缩略图面板大小，也是图片大小
	private int blockWidth = -1,blockHeight  = -1;//DisplayBlock显示块大小
	private int padding = 10;//显示块面板与缩略图面板之间的内边距
	private int infoHeight = 50;//信息显示区域的高度
	private VideoInfo videoInfo = null;
	//计算主面板应该多高,主需要一个静态的即可，该函数只在添加主面板时使用一次
	public static int getTotalHeight() {
		//blockHeight = tnHeight+3*padding+infoHeight;
		return 3*(240+3*10+50);//共显示3行，每行3个
	}
	//返回当前视频显示块对象内的videoInfo对象
	public VideoInfo getVideoInfo() {
		return videoInfo;
	}
	//HashMap<>()存储视频时长等信息
	public VideoPanel(VideoInfo videoInfo) {
		
		this.videoInfo = videoInfo;
		//计算DisplayBlock显示块大小
		blockWidth = tnWidth+2*padding;
		blockHeight = tnHeight+3*padding+infoHeight;
		this.setPreferredSize(new Dimension(blockWidth,blockHeight));//显示块面板大小，上下左右各比缩略图面板大padding
		this.setBackground(SelectedVideoPanel.getNoSelectionColor());//显示块面板初始背景
		this.setLayout(null);//绝对布局
		//鼠标点击时呈现出该显示块被选择的效果
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				System.out.println("Click");
				//从事件里反向获取当前组件的索引
				VideoPanel thisDisplayBlock = (VideoPanel)e.getSource();
				SelectedVideoPanel.changeSelectionBlock(thisDisplayBlock);//选块切换
			}
		});
		BufferedImage bufferedImage = videoInfo.getBufferedImage();
		//在显示块上添加缩略图面板
		ThumbnailPanel thumbnailPanel = new ThumbnailPanel(tnWidth,tnHeight,bufferedImage);
		thumbnailPanel.repaint();//在显示块上画出缩略图
		this.add(thumbnailPanel);//将缩略图面板加到显示块上去
		thumbnailPanel.setBounds(padding,padding, tnWidth,tnHeight);//定位缩略图面板的位置,同时设置大小
		//在显示块上添加文本信息面板
		InfoPanel infoPanel = new InfoPanel(
				videoInfo.getVideoName(),
				videoInfo.getDuration(),
				videoInfo.getResolution());
		this.add(infoPanel);
		infoPanel.setBounds(padding, tnHeight+2*padding, tnWidth,infoHeight);//定位信息面板的位置,同时设置大小
		//新加一个很细的面板，遮住thumbnailPanel和infoPanel之间的那块背景色
		JPanel gapPanel = new JPanel();
		gapPanel.setBackground(new Color(166, 166, 166));
		this.add(gapPanel);
		gapPanel.setBounds(padding, tnHeight+padding, tnWidth, padding);
	}
}

/*
 * 缩略图面板，不用设置布局，图片直接画上去
 * */
class ThumbnailPanel extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2070125390008961705L;
	private Image scaledImage = null;
	//构造函数
	public ThumbnailPanel(int tnWidth , int tnHeight, BufferedImage bufferedImage) {
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
class InfoPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2611671258325196571L;

	public InfoPanel(String videoName,String duration,String resolution) {
		this.setBackground(new Color(196,196,196));
		//this.setLayout(new GridLayout(3, 1, 0, 3));//行数 列数 水平间隔 竖直间隔
		this.setLayout(null);
		JLabel jLabelVideoName = new JLabel("视频名："+videoName);
		JLabel jLabelDuration = new JLabel("时长："+duration);
		JLabel jLabelResolution = new JLabel("分辨率："+resolution);
		jLabelVideoName.setFont(new Font("Dialog", Font.BOLD, 15));
		jLabelDuration.setFont(new Font("Dialog", Font.BOLD, 15));
		jLabelResolution.setFont(new Font("Dialog", Font.BOLD, 15));
		this.add(jLabelVideoName);
		this.add(jLabelDuration);
		this.add(jLabelResolution);
		//信息面板高50.宽320
		jLabelVideoName.setBounds(2, 2, 320, 15);
		jLabelDuration.setBounds(2, 18, 320, 15);
		jLabelResolution.setBounds(2, 34, 320, 15);
	}
}


/*静态变量和全局方法，记录当前被选中的块*/
class SelectedVideoPanel{
	private static VideoPanel selectedVideoPanel = null;//被选择视频块对象的引用
	private static Color noSelectionColor = new Color(184,184,184);//未选择时颜色
	private static Color selectionColor = new Color(137,186,251);//被选择时颜色
	/*获得被选择的视频块*/
	public static VideoPanel getSelectedVideoPanel() {
		return selectedVideoPanel;
	}
	public static void changeSelectionBlock(VideoPanel videoPanel) {
		if(selectedVideoPanel != null)//第一次清空时，它没有上一块
			selectedVideoPanel.setBackground(noSelectionColor);//清空上一块颜色
		videoPanel.setBackground(selectionColor);//设置本块颜色
		selectedVideoPanel = videoPanel;//本块变为上一块
	}
	public static Color getNoSelectionColor() {
		return noSelectionColor;
	}
	/**
	 * 该函数将lastBlock置为Null
	 */
	public static void resetSelectedBlock() {
		selectedVideoPanel = null;
	}
}