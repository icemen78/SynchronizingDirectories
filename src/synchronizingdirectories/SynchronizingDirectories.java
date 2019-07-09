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
    ErrorEntry      - Класс инкапсулирующий пару объектов: ошибку, произошедшую при попытке синхронизации
                      объекта FileEnrty и соответствующее имя файла (РАЗРАБОТКА НЕ ЗАВЕРШЕНА).   
*/

package synchronizingdirectories;

import java.awt.Color;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author icemen78
 */
public class SynchronizingDirectories extends JFrame {
    private static final String version="011 beta";
    private volatile SynchFolders sf = null;
    JTextField sourcePath;
    JTextField targetPath;
    JButton ok;
    JTextArea info;
    JCheckBox hiddenVis;
//    Box box4;
//    Box mainBox;
    final JProgressBar progressBar;
    final JProgressBar progressVBar;
    int prefferedwidth;
    JRadioButton radS1Button, radS2Button;
    
    /**
     * @param title
     */
    public SynchronizingDirectories(String title){
        super(title+ " (v. "+version+")");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                cancel(true);
            }
        });
        
        JLabel sourcePathlabel = new JLabel("Первичное местоположение");
        JLabel targetPathlabel = new JLabel("Вторичное местоположение");
        targetPathlabel.setPreferredSize(sourcePathlabel.getPreferredSize());
        //JTextField sourcePath
        sourcePath = new JTextField(50);
        sourcePath.setEnabled(false);
        sourcePath.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                selectSource();
            }
        });
        //JTextField targetPath
        targetPath = new JTextField(50);
        targetPath.setEnabled(false);
        targetPath.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                selectTarget();
            }
        });
        //JTextArea info
        info = new JTextArea();
        info.setEditable(false);
        info.setRows(7);
        info.setLineWrap(true);
        JScrollPane sb = new JScrollPane(info);
        sb.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JButton sourceSelect = new JButton("...");
        sourceSelect.setFocusable(false);
        sourceSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectSource();
            }
        });
        JButton targetSelect = new JButton("...");
        targetSelect.setFocusable(false);
        targetSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTarget();
            }
        }); 
        //JButton ok
        ok = new JButton("Синхронизация");
        ok.setFocusable(false);
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
//                System.out.println(ae.toString());
//                if(mainBox!=null){
                    ok.setEnabled(false);
                    info.setForeground(Color.black);
                    info.setText("подготовка каталогов ....");
                    
//                    mainBox.remove(box4);
//                    prefferedwidth=getContentPane().setWidth();
//                    pack();
//                }
                int scriptType=-1;
                File sdir = new File(sourcePath.getText());
                File tdir = new File(targetPath.getText());
                if (!sdir.exists() || !tdir.exists()) {
                    showReport("Укажите корректную исходную и целевую директории ...", true);
                }else if (radS1Button.isSelected()) {
                    scriptType=SynchFolders.SF_AGREGATE;
                }else if (radS2Button.isSelected()) {
                    scriptType=SynchFolders.SF_CONTROL_FOLDER;
                }else {
                    showReport("Не определен сценарий обработки", true);
                }
                if (scriptType!=(-1)) {
                    sf = sinc(sourcePath.getText(),targetPath.getText(),scriptType);
                }
            }
        });
        JButton cancel = new JButton("Отмена");
        cancel.setFocusable(false);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                cancel(false);
            }
        });
        progressVBar = new JProgressBar();
        progressVBar.setStringPainted(true);
        progressVBar.setMinimum(0);
        progressVBar.setMaximum(100);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        //JRadioButton
        radS1Button = new JRadioButton("Объединение", false);
        radS1Button.setFocusable(false);
        radS2Button = new JRadioButton("Замещение", true);
        radS2Button.setFocusable(false);
        ButtonGroup radButtons = new ButtonGroup();
        radButtons.add(radS1Button);
        radButtons.add(radS2Button);
        
        
        
        Box box1 = Box.createHorizontalBox();
        box1.add(sourcePathlabel);
        box1.add(Box.createHorizontalStrut(6));
        box1.add(sourcePath);
        box1.add(Box.createHorizontalStrut(6));
        box1.add(sourceSelect);
        Box box2 = Box.createHorizontalBox();
        
        box2.add(targetPathlabel);
        box2.add(Box.createHorizontalStrut(6));
        box2.add(targetPath);
        box2.add(Box.createHorizontalStrut(6));
        box2.add(targetSelect);
        
        Box box3 = Box.createHorizontalBox();
        
            Box box3v1 = Box.createVerticalBox();
            box3v1.add(Box.createVerticalGlue());
            box3v1.add(progressVBar);
            box3v1.add(Box.createVerticalStrut(12));
            box3v1.add(progressBar);

            Box box3v2 = Box.createVerticalBox();
                Box box3v2g1 = Box.createHorizontalBox();
                box3v2g1.add(radS1Button);
                box3v2g1.add(Box.createHorizontalStrut(17));
                box3v2g1.add(radS2Button);

                Box box3v2g2 = Box.createHorizontalBox();
                    Box box3v2g2v1 = Box.createVerticalBox();
                    box3v2g2v1.add(Box.createVerticalGlue()); 
                    box3v2g2v1.add(ok);
                    Box box3v2g2v2 = Box.createVerticalBox();
                    box3v2g2v2.add(Box.createVerticalGlue()); 
                    box3v2g2v2.add(cancel);
                box3v2g2.add(box3v2g2v1);
                box3v2g2.add(Box.createHorizontalStrut(6));
                box3v2g2.add(box3v2g2v2);
                Box box3v2g3 = Box.createHorizontalBox();
                    hiddenVis = new JCheckBox();
                    hiddenVis.setText("use hidden objects");
                    hiddenVis.setSelected(SynchFolders.SF_CHECK_HIDDEN_DEFAULT);
                    hiddenVis.setFocusable(false);
                    box3v2g3.add(hiddenVis);
//            box3v2.add(Box.createVerticalGlue());
//            Dimension dim;
//            dim=box3v2.getPreferredSize();
            box3v2.add(Box.createVerticalGlue());
            box3v2.add(box3v2g1);
            
            
//            box3v2.add(Box.createVerticalStrut(12));
            box3v2.add(box3v2g2);
            box3v2.add (box3v2g3);
//            box3v2.setPreferredSize(new Dimension ((int)box3v2.getPreferredSize().getWidth(), (int)box3v1.getPreferredSize().getHeight()));
        
        box3.add(box3v1);
        box3.add(Box.createHorizontalStrut(12));
        box3.add(box3v2);
//        box3v2.setMaximumSize(new Dimension((int)(box3.getPreferredSize().getWidth()/3),(int)box3v2.getPreferredSize().getHeight()));

        Box box4 = Box.createHorizontalBox();
        box4.add(sb);
        Box mainBox = Box.createVerticalBox();
        mainBox.setBorder(new EmptyBorder(12,12,12,12));
        mainBox.add(box1);
        mainBox.add(Box.createVerticalStrut(12));
        mainBox.add(box2);
        mainBox.add(Box.createVerticalStrut(17));
        mainBox.add(box3);
        mainBox.add(Box.createVerticalStrut(12));
        mainBox.add(box4);
        
        setContentPane(mainBox);
        pack();
//        prefferedwidth=this.getPreferredSize().width;
//        prefferedwidth=this.getContentPane().;
//        Dimension dim = new Dimension(box3.getPreferredSize().width-100,info.getPreferredSize().height);
//        box4.setSize(dim);
        setResizable(false);
        this.setLocationRelativeTo(null);
//        sourcePath.setText("C:\\JAVA\\Distrib\\SincFolders\\_jar");
//        targetPath.setText("E:\\Java\\Distrib\\SincFolders\\_jar");
//        sourcePath.setText("C:\\Temp\\sinch\\src");
//        targetPath.setText("C:\\Temp\\sinch\\tgt");
        this.setVisible(true);
    }

    public SynchFolders sinc(String fileSource, String fileTarget, int scriptType){
//        fileSource="C:\\Temp\\sinch\\src";
//        fileTarget="C:\\Temp\\sinch\\tgt";
//        scriptType=SynchFolders.SF_CONTROL_FOLDER;
        
        SynchFolders retval=null;
        try {
            if (fileSource.equals(fileTarget)) {throw new Exception("<NoPrintStackTrace>Директории совпадают");}
            if (fileSource.contains(fileTarget)||fileTarget.contains(fileSource)) {throw new Exception("<NoPrintStackTrace>Директории являются частью друг друга");}
            File src = new File(fileSource);
            File tgt = new File(fileTarget);
            
            progressBar.setValue(0);
            progressVBar.setValue(0);
            
            retval = new SynchFolders(src, tgt, scriptType);
            if (hiddenVis.isSelected()!=SynchFolders.SF_CHECK_HIDDEN_DEFAULT){retval.setCheckHidden(hiddenVis.isSelected());}
            retval.addListener(new SinchListener() {
                @Override
                public void progressed(final double percent) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                        progressBar.setValue((int)Math.round(percent*100));
                        }
                    });
                }
                @Override
                public void progresstotal(final double percent) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                        progressVBar.setValue((int)Math.round(percent*100));
                        }
                    });
                }
                @Override
                public void reportready(String text, boolean important) {
                    showReport(text,important);
                    sf = null;
                }
                @Override
                public void started() {
//                    showReport("синхронизация каталогов ....", false);
                    ok.setEnabled(false);
                }

                @Override
                public void prepared(String text, boolean important) {
                    showReport(text,important);
                }
            });
            retval.synch();
        }catch (Exception e) {
            showReport(getErrorStack(e),true);
        }
        return retval;
    }
    
    public static void main(String[] args) {
        JFileChooser_locate();
        SynchronizingDirectories mainform = new SynchronizingDirectories("Синхронизация каталогов");
    }
    private void showReport(String text, boolean Important) {
        info.setText(text);
        info.setCaretPosition(1);
        info.setForeground(Important?Color.red:Color.black);
        ok.setEnabled(true);
//        mainBox.add(box4);
//        pack();
        
    }
    public static void JFileChooser_locate() {
        //FileChooser локализация
//        JFileChooser.UIManager.put("FileChooser.frameTitleText", "FFFFFF");
        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.lookInLabelText", "Смотреть в");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файла");

        UIManager.put("FileChooser.saveButtonText", "Сохранить");
        UIManager.put("FileChooser.saveButtonToolTipText", "Сохранить");
        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.openButtonToolTipText", "Открыть");
        UIManager.put("FileChooser.cancelButtonToolTipText", "Отмена");

        UIManager.put("FileChooser.lookInLabelText", "Папка");
        UIManager.put("FileChooser.saveInLabelText", "Папка");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файлов");

        UIManager.put("FileChooser.upFolderToolTipText", "На один уровень вверх");
        UIManager.put("FileChooser.newFolderToolTipText", "Создание новой папки");
        UIManager.put("FileChooser.listViewButtonToolTipText", "Список");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Таблица");
        UIManager.put("FileChooser.fileNameHeaderText", "Имя");
        UIManager.put("FileChooser.fileSizeHeaderText", "Размер");
        UIManager.put("FileChooser.fileTypeHeaderText", "Тип");
        UIManager.put("FileChooser.fileDateHeaderText", "Изменен");
        UIManager.put("FileChooser.fileAttrHeaderText", "Атрибуты");
        UIManager.put("FileChooser.acceptAllFileFilterText", "Все файлы");

        UIManager.put("FileChooser.viewMenuLabelText", "Просмотр");
        UIManager.put("FileChooser.detailsViewActionLabelText", "Таблица");
        UIManager.put("FileChooser.listViewActionLabelText", "Список");
        UIManager.put("FileChooser.refreshActionLabelText", "Обновить");
        UIManager.put("FileChooser.newFolderActionLabelText", "Новая папка");
        UIManager.put("FileChooser.win32.newFolder", "Новая папка");
        UIManager.put("FileChooser.win32.newFolder.subsequent", "Новая папка ({0})");

        UIManager.put( "FileChooser.readOnly", Boolean.TRUE);
    }
    private void setUpdateUI(JFileChooser chooser) {
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(false);
    }
    public static String getErrorStack(Exception e) {
        
        
        System.out.println(e.getMessage());
        String retval="Ошибка выполнения: "+e.toString();
        String err=e.getMessage();
        if (err.startsWith("<NoPrintStackTrace>")) {
            retval="Ошибка выполнения: " + err.substring("<NoPrintStackTrace>".length());
        }else {
            for(StackTraceElement ste : e.getStackTrace()){
                retval += "\n" + ste.toString();
            }            
        }
        return retval;
    }
    private void selectSource() {
        JFileChooser fileopen = new JFileChooser();  
        setUpdateUI(fileopen);
        fileopen.setCurrentDirectory(new File(sourcePath.getText()).getParentFile());
        fileopen.setDialogTitle("Первичное местоположение");
        int ret = fileopen.showDialog(null, null);                
        if (ret == JFileChooser.APPROVE_OPTION) {
            sourcePath.setText(fileopen.getSelectedFile().getAbsolutePath());
        }
    }
    private void selectTarget() {
        JFileChooser fileopen = new JFileChooser();
        setUpdateUI(fileopen);
        File f = new File(targetPath.getText());
        fileopen.setCurrentDirectory((f.exists())?f.getParentFile():new File(sourcePath.getText()).getParentFile());
        fileopen.setDialogTitle("Вторичное местоположение");
        int ret = fileopen.showDialog(null, null);                
        if (ret == JFileChooser.APPROVE_OPTION) {
            targetPath.setText(fileopen.getSelectedFile().getAbsolutePath()); 
        }
    }
    private void cancel(boolean withExit) {
        if (sf != null) {
            sf.interrupt();
            sf=null;
        }else {
            withExit=true; 
        }
        if (withExit) {
           setVisible(false);
           System.exit(0); 
        }
    }
}
