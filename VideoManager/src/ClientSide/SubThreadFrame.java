package ClientSide;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

//继承了JFrame

public class SubThreadFrame {
	private static final long serialVersionUID = -4932178739506938652L;
	private JFrame subFrame = null;
	private JPanel contentPane = null;
	private JTextArea jtextArea = null;

	/**
	 * Create the frame.
	 */
	public JFrame createSubFrame() {
		subFrame = new JFrame();
		subFrame.setBounds(300, 200, 547, 356);
		contentPane = new JPanel();
		subFrame.getContentPane().add(contentPane, BorderLayout.CENTER);
		contentPane.setLayout(null);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		// 不可用EXIT_ON_CLOSE，否则整个程序直接关闭。我们只需要这个子窗口关闭
		// 参考: http://www.singlex.net/2273.html
		subFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 12, 509, 290);
		contentPane.add(scrollPane);

		jtextArea = new JTextArea();
		jtextArea.setEditable(false);
		scrollPane.setViewportView(jtextArea);
		subFrame.setVisible(true);
		return subFrame;
	}

	/*
	 * 获取显示组件
	 */
	public JTextArea getJTextArea() {
		return jtextArea;
	}

	/*
	 * 获取窗口框架对象
	 */
	public JFrame getJFrame() {
		return subFrame;
	}
}
