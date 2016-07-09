/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.filechooser.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
/**
 *
 * @author Kevin
 */
@SuppressWarnings("serial")
public class WindowChooser extends javax.swing.JFrame {

    private String midiPath;
	public WindowChooser() {
		readFromProperty(System.getProperty("user.dir"));
        initComponents();
        initComponents2();
    }
    
    private void initComponents2(){
        // Set window start up location as screen center
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width / 2 - getSize().width/ 2, dim.height / 2 - getSize().height / 2);
        setVisible(true);
         
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WindowChooser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WindowChooser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WindowChooser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WindowChooser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jFileChooser1 = new javax.swing.JFileChooser(midiPath);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Maiandra GD", 0, 18)); // NOI18N
        jLabel1.setText("Select MIDI File");

        FileFilter filter = new FileNameExtensionFilter("MIDI file", "mid", "midi");
        jFileChooser1.addChoosableFileFilter(filter);
        jFileChooser1.setFileFilter(filter);
        jFileChooser1.addActionListener((java.awt.event.ActionEvent evt) -> {
            jFileChooser1ActionPerformed(evt);
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jFileChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 720, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(299, 299, 299))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jFileChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 357, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(54, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jFileChooser1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooser1ActionPerformed
        if (evt.getActionCommand() .equals(JFileChooser.APPROVE_SELECTION) ) {
            if(fileExists()) {
                try {
                	midiPath = jFileChooser1.getSelectedFile().getPath().replace(jFileChooser1.getSelectedFile().getName(), "");
                	System.out.println(midiPath);
                	writeToPropertyFile(System.getProperty("user.dir"));
                    new WindowOption(jFileChooser1.getSelectedFile());
                } catch (InvalidMidiDataException ex) {
                    Logger.getLogger(WindowChooser.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(WindowChooser.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MidiUnavailableException ex) {
                    Logger.getLogger(WindowChooser.class.getName()).log(Level.SEVERE, null, ex);
                }
                     this.dispose();
            } else {
                JOptionPane.showMessageDialog(jFileChooser1, "Please select MIDI file.");
            }
        } else if (evt.getActionCommand() .equals(JFileChooser.CANCEL_SELECTION)) {
            System.exit(0);
        }
    }//GEN-LAST:event_jFileChooser1ActionPerformed

    private void readFromProperty(String path) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(path + "\\config.properties");
			prop.load(input);
			midiPath = prop.getProperty("midiPath");
			input.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void writeToPropertyFile(String path) {
		Properties prop = new Properties();
		OutputStream output = null;
		try {
			FileInputStream input = new FileInputStream(path + "\\config.properties");
			prop.load(input);
			prop.setProperty("midiPath",midiPath);
			input.close();
			// save properties to project root folder
			output = new FileOutputStream(path + "\\config.properties");
			prop.store(output, null);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private boolean fileExists() {
        File file = jFileChooser1.getSelectedFile();
        return file.exists()
                && file.isFile()
                && file.toString().substring(file.toString().indexOf(".")).contains("mid");
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
