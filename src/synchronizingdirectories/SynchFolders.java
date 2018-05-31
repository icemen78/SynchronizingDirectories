/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
    Класс синхронизирует две дирректории.
    Инкапсулирует данные о дирректориях, типе синхронизации (final -константы).
    Результат предварительного сравнения содержит в массиве объектов FileEnrty.
    Синхронизируемые дирректории определяются в след. порядке: 
*/
package synchronizingdirectories;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author icemen78
 */
public class SynchFolders implements Runnable{
    //только копирование отсутствующих директорий/файлов с заменой старых файлов новыми
    public static final int SF_AGREGATE = 0;
    //условное копировани(удаление) в target из source с заменой старых файлов новыми в source
    public static final int SF_CONTROL_FOLDER = 1;
    //безусловное копирование(удаление) из более свежей папки в устаревшую
    public static final int SF_RELATIVE = 2;
    
    public final File src;
    public final File tgt;
    private final int type;
    private Date srcDate, tgtDate;
    private long srcMaxDate, tgtMaxDate;
    private final String fileInf = "_sync";
    
    private boolean prepareFlag = false;
    private boolean sinchFlag = false;
    private TreeSet files;
    private TreeSet ferrors;
    private BufferedInputStream rbuffSource, rbuffTarget;
    private BufferedOutputStream wbuffSource, wbuffTarget;
    
    private static final int BUFFER_SIZE = 8388608;
    
    public SynchFolders(File dirSource, File dirTarget, int typeSinch) throws Exception{
        if ((!dirSource.exists() || !dirTarget.exists()) || (dirSource.isFile() || dirTarget.isFile())) {
            throw new Exception("filedirectory is not exist");
        }
        
        src = dirSource;
        tgt = dirTarget;
        type = typeSinch;
        
        File sinf, tinf;    //файлы синхронизации
        String skey, tkey;  //ключи синхронизации (ниаменование файлового архива)
        Date sdate, tdate;  //дата синхронизации
        
        switch (type) {
            case SF_RELATIVE:
                sinf = new File(dirSource+File.separator+fileInf);
                tinf = new File(dirTarget+File.separator+fileInf);
                if (!(sinf.exists() && sinf.exists())) {
                    throw new Exception("system file is not exist");
                }
                BufferedReader s1 = new BufferedReader(new FileReader(sinf));
                BufferedReader s2 = new BufferedReader(new FileReader(tinf));
                skey = s1.readLine();
                tkey = s2.readLine();
                if (!skey.equals(tkey)) {
                    throw new Exception("keys not equals");
                }
                try {
                    sdate = new Date(Long.parseLong(s1.readLine()));
                    tdate = new Date(Long.parseLong(s2.readLine()));
                }catch (NumberFormatException ex) {
                    throw new Exception("dates in sync-files is not valid");
                }
                break;
            default:
                sdate = null;
                tdate = null;
                break;
        }
        srcDate = sdate;
        tgtDate = tdate;
        files = new TreeSet(new CompareShortFName());
        ferrors = new TreeSet(new CompareShortFName());
        srcMaxDate = dirSource.lastModified();
        tgtMaxDate = dirTarget.lastModified();
    }
    
    @Override
    public void run() {
        if (!prepareFlag) {
            files.clear();
            ferrors.clear();
            files = preparing(src,tgt);
            prepareFlag = true;
        }else {
            ferrors.clear();
            ferrors = synchronizing();
            sinchFlag = true;
        }
    }
    
    public void prepare() {
        Thread t = new Thread(this, "prepare");
        //запуск рекурсивного метода сбора информации о каталогах
        t.start();
    }
    public void synch() {
        if (prepareFlag) {
            Thread t = new Thread(this, "sinch");
            //запуск рекурсивного метода обработки о каталогов
            t.start();
        }
    }
    private TreeSet preparing(File src, File tgt) {
        TreeSet retval = new TreeSet();
        
        TreeMap slf = new TreeMap();
        TreeMap tlf = new TreeMap();
        
        for (File f : src.listFiles()) {
            slf.put(f.getName(),f);
            if (f.lastModified()>srcMaxDate) srcMaxDate = f.lastModified();
        }
        for (File f : tgt.listFiles()) {
            tlf.put(f.getName(),f);
            if (f.lastModified()>tgtMaxDate) tgtMaxDate = f.lastModified();
        }
        
        TreeSet srclist = new TreeSet(slf.keySet());
        TreeSet tgtlist = new TreeSet(tlf.keySet());
        tgtlist.removeAll(srclist);
        srclist.addAll(tgtlist);
        Iterator iterator = srclist.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            if (!key.equals("_sync")) {
                FileEntry fe = new FileEntry((File)slf.get(key),(File)tlf.get(key), this);
                if (fe.isFoldersExists()) {
                    //разбираем пару папок 
                    retval.addAll(preparing(fe.fileSRC, fe.fileTGT));
                } else if (fe.isDateEquals()) {
                    //
                } else {
                    //добавляем одиночные папки и одиночные или парные файлы
                    retval.add(fe);
                }
            }
        }
        if (srcDate==null || srcMaxDate>srcDate.getTime()) {
            srcDate = new Date(srcMaxDate);
        }
        if (tgtDate==null || tgtMaxDate>tgtDate.getTime()) {
            tgtDate = new Date(tgtMaxDate);
        }
        return retval;
    }  
    private TreeSet synchronizing() {
        TreeSet retval = new TreeSet();
        Iterator iterator = files.iterator();
        while (iterator.hasNext()) {
            FileEntry fe = (FileEntry)iterator.next();
            try {
                switch (fe.getState()) {
                    case FileEntry.FE_MAST_NEW:
                        //рекурсивно копируем файловые объекты
                        copyFObject(fe.getSourceFObj(),fe.getDistanceDir());
                        break;
                    case FileEntry.FE_MAST_REPLACE:
                        System.out.println(fe.toString() + " (замена)");
                        File source = fe.getSourceFObj();
                        File distance = new File (fe.getDistanceDir()+File.separator+source.getName());
                        distance.createNewFile();
                        //заменяем файлы
                        FileInputStream fis = null;
                        FileOutputStream fos = null;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesCopied;
                        try {
                            fis = new FileInputStream(source);
                            fos = new FileOutputStream(distance);
                            while ((bytesCopied = fis.read(buffer)) > 0) {fos.write(buffer, 0, bytesCopied);}
                            fis.close();
                            fos.close();
                            distance.setLastModified(source.lastModified());                            
                        }catch (Exception e) {
                            if (fis!=null)fis.close();
                            if (fos!=null)fos.close();
                            throw e;
                        }
                        break;
                    case FileEntry.FE_MAST_DELETE:
                        //рекурсивно удаляем файловые объекты
                        deleteFObject(fe.getSourceFObj());
                        break;
                }
            } catch (Exception e) {
                ferrors.add(new ErrorEntry(e,fe.getSourceFObj().getName(),fe.getState()));
            }
        }
        return retval;
    }

    private void copyFObject(File sobject, File tobject) throws Exception {
        //sfolder - существующий файловый объект-источник
        //tfolder - существующая родительсткий объект-контейнер
        String newFObjName = tobject.getAbsolutePath()+File.separator+sobject.getName();
        System.out.println(newFObjName);
        File newFObj = new File(newFObjName);
        if (sobject.isDirectory()) {
            newFObj.mkdir();
            for (File f : sobject.listFiles()) {
                try {
                    copyFObject(f,newFObj);
                }catch (Exception e) {
                    ferrors.add(new ErrorEntry(e,f.getAbsolutePath(),FileEntry.FE_MAST_NEW));
                }
            }
        }else {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesCopied;
            try {
                fis = new FileInputStream(sobject);
                fos = new FileOutputStream(newFObj);
                while ((bytesCopied = fis.read(buffer)) > 0) {fos.write(buffer, 0, bytesCopied);}
                fis.close();
                fos.close();
            }catch (Exception e) {
                if (fis!=null)fis.close();
                if (fos!=null)fos.close();
                throw e;
            }
        }
        //установка аттрибутов
        newFObj.setLastModified(sobject.lastModified());        
    }
    
    private void deleteFObject(File sobject) throws Exception{
        if (sobject.isFile()) {
            sobject.delete();
        }else {
            for (File f : sobject.listFiles()) {
                try {
                    deleteFObject(f);
                }catch (Exception e) {
                    ferrors.add(new ErrorEntry(e,f.getAbsolutePath(),FileEntry.FE_MAST_DELETE));
                }                
            }
        }
    }
    
    public boolean isPrepared() {
        return prepareFlag;
    }
    public boolean isSyncronized() {
        return sinchFlag;
    }
    public TreeSet getPreparedList() {
        return files;
    }
    public TreeSet getFileErrorsList() {
        return ferrors;
    }
    public int getType() {
        return type;
    }
    public Date getSrcDate() {
        return srcDate;
    }
    public Date getTgtDate() {
        return tgtDate;
    }
    public boolean isSinchronizable() {
        boolean retval = false;
        if (prepareFlag) {
            retval = true;
            Iterator i = files.iterator();
            while (i.hasNext()) {
                FileEntry fe = (FileEntry)i.next();
                if (!fe.isSinchronizable()) {
                    retval = false;
                    break;
                }
            }
        }
        return retval;
    }
    private void updateStoredInf() {
        
    }
}
