/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
    Программа синхронизации двух директорий (точка входа).
    SinchFolders    - главный класс, объект, инкапсулирующий входные данные и
                      и результат.
    FileEntry       - класс, инкапсулирующий пару сравниваемых файловых объектов
                      для принятия решения о типе их синхранизации.
*/

package synchronizingdirectories;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import static java.lang.Thread.sleep;
import java.util.Iterator;

/**
 *
 * @author icemen78
 */
public class SynchronizingDirectories  extends Frame {
    /**
     * @param title
     */
    public SynchronizingDirectories(String title) {
        super(title);
        setLayout(new FlowLayout());
        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
        Label lSource = new Label("Исходная дирректория");
        TextField tfSource = new TextField(15);
        tfSource.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                
            }
        });
        add(lSource);
        add(tfSource);
//        FileDialog s = new java.awt.FileDialog(this);
//        s.gett(FileDialog.LOAD);
//        s.setVisible(true);
//        String f = s.getDirectory();
//        System.out.println(f);
        setBackground(Color.LIGHT_GRAY);
        setSize(640,480);
        init();
        setLocationRelativeTo(null);
        setVisible(true);        
    }
    public final void init() {
        String fileSource="C:\\Temp\\src";
        String fileTarget="C:\\Temp\\target";
        
        File src = new File(fileSource);
        File tgt = new File(fileTarget);
        
        try {
            SynchFolders sf = new SynchFolders(src, tgt, SynchFolders.SF_AGREGATE);
            sf.prepare();
            while (!sf.isPrepared()) {
                sleep(5);
            }
            if (sf.isSinchronizable()) {
                sf.synch();
                while (!sf.isSyncronized()) {
                    sleep(100);
                }                
            }

            Iterator i = sf.getPreparedList().iterator();
            while (i.hasNext()) {
                FileEntry fe = (FileEntry)i.next();
//                System.out.println(fe.getState() + "   " + fe.getSourceFObj()+"   "+fe.getDistanceDir());
            }
            System.out.println("Синхронизируется объектов: " + sf.getPreparedList().size());
            System.out.println("Количество ошибок: " + sf.getFileErrorsList().size());
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
    public static void main(String[] args) {
        SynchronizingDirectories mainform = new SynchronizingDirectories("Синхронизация каталогов");
    }
}
