import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ShowPNG extends JFrame {
    /**
     * 
     */
    private static final long serialVersionUID = 5359007893935207888L;
    private String arg;
    
    public ShowPNG(String arg) {
        if (arg == null ) {
            this.arg = "C:/yitzchak/Workspace/Server/bin/" + "umdlogo.jpg";
        }
        
        JTextField mTitle = new JTextField("Retrieving Data...");
        this.setTitle(mTitle.getText());
    } 
    
    public void setImage() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        ImageIcon icon = new ImageIcon(arg);
        
        JLabel label = new JLabel();
        label.setIcon(icon);
        panel.add(label, BorderLayout.CENTER);
        //panel.setSize(new Dimension(500, 500));
        panel.setPreferredSize(new Dimension(500, 500));
        this.getContentPane().add(panel);
        revalidate();
    }
}
