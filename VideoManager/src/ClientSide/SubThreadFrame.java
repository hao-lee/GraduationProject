package ClientSide;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

//继承了JFrame

public class SubThreadFrame extends JFrame {
	private static final long serialVersionUID = -4932178739506938652L;
	private JPanel contentPane;
	JTextArea jtextArea = null;
	/**
	 * Test the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					SubThreadFrame frame = new SubThreadFrame();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	/**
	 * Create the frame.
	 */
	public SubThreadFrame(String title) {
		setBounds(300, 200, 547, 356);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		setTitle(title);
		//不可用EXIT_ON_CLOSE，否则整个程序直接关闭。我们只需要这个子窗口关闭
		//参考: http://www.singlex.net/2273.html
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 12, 509, 290);
		panel.add(scrollPane);
		
		jtextArea = new JTextArea();
		jtextArea.setEditable(false);
		scrollPane.setViewportView(jtextArea);
		setVisible(true);
	}
	public JTextArea getJTextArea() {
		return jtextArea;
	}
}
