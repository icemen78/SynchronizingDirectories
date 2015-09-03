/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
    Класс инкапсулитует пару сравниваемых файловых объектов приводя результат к
    перечислимой константе относительно 
*/
package synchronizingdirectories;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

/**
 *
 * @author icemen78
 */
public class FileEntry  implements Comparable {
    public static final int FE_UNKNOWN = 0;
    public static final int FE_MAST_NEW = 1;
    public static final int FE_MAST_REPLACE = 2; //только для файлов
    public static final int FE_MAST_DELETE = 3;
    
    public final File fileSRC, fileTGT;
    public final Date ds, dt;
    public final SynchFolders parent;
    
    private int state = FE_UNKNOWN;
    
    private File srcFObj = null;
    private File tgtDir = null;
    private boolean prepFlag = false;
    private Exception err = null;
    
    public FileEntry(File s, File t, SynchFolders sf) {
        fileSRC = s;
        if (s==null) ds=null; else ds = new Date(s.lastModified());
        fileTGT = t;            
        if (t==null) dt=null; else dt = new Date(t.lastModified());
        parent = sf;         
    }

    public boolean isFoldersExists() {
        return ((fileSRC==null)||(fileTGT==null) ? false : fileSRC.isDirectory());
    }
    public boolean isDateEquals() {
        return (((ds!=null)&&(dt!=null))&&(ds.equals(dt)));
    }
    public boolean isFolders() {
        return ((fileSRC!=null) ? fileSRC.isDirectory() : fileTGT.isDirectory());
    }
    public File getSourceFObj() {
        if (!prepFlag) prepSolution();
        return srcFObj;
    }
    public File getDistanceDir() {
        if (!prepFlag) prepSolution();
        return tgtDir;
    }
    private File calcNewDistance() {
        String retval = srcFObj.getParent();
        String source = parent.src.getAbsolutePath();
        String target = parent.tgt.getAbsolutePath();
        if (retval.startsWith(source)) {
            retval = target+retval.substring(source.length());
        } else {
            retval = source+retval.substring(target.length());
        }
        return new File(retval);
    }
    private void prepSolution() {
        //в соответствии с логикой приложения, копируемые объекты класса могут
        //быть только парами файлов или одиночными папками
        srcFObj=fileSRC;
        tgtDir=null;
        switch (parent.getType()) {
            case SynchFolders.SF_AGREGATE:
                if ((fileSRC!=null) && (fileTGT!=null)) {
                    //одноименные файлы
                    if (ds.compareTo(dt)<0) {
                        srcFObj=fileTGT;
                    }
                    state = FileEntry.FE_MAST_REPLACE;
                }else {
                    //одиночная папка или файл
                    srcFObj = (fileSRC!=null ? fileSRC : fileTGT);
                    state = FileEntry.FE_MAST_NEW;
                }
                break;
            case SynchFolders.SF_CONTROL_FOLDER:
                if ((fileSRC!=null) && (fileTGT!=null) && ds.compareTo(dt)<0) {
                    srcFObj=fileTGT;
                }
                if ((fileSRC!=null) && (fileTGT!=null)) {
                    //одноименные файлы
                    state = FileEntry.FE_MAST_REPLACE;
                }else if (fileSRC==null) {
                    srcFObj=fileTGT;
                    state = FileEntry.FE_MAST_DELETE;
                }else {
                    state = FileEntry.FE_MAST_NEW;
                }
                break;
            case SynchFolders.SF_RELATIVE:
                if (parent.getSrcDate().compareTo(parent.getTgtDate())<0) {
                    srcFObj=fileTGT;
                }
                if ((fileSRC!=null) && (fileTGT!=null)) {
                    //одноименные файлы
                    state = FileEntry.FE_MAST_REPLACE;
                }else if (fileSRC==null) {
                    srcFObj=fileTGT;
                    state = FileEntry.FE_MAST_DELETE;
                }else {
                    state = FileEntry.FE_MAST_NEW;
                }
                break;
            default:
                System.out.println("default");
        }
        //вычисляем tgtDir
        tgtDir = calcNewDistance();
        prepFlag=true;
    }
    public Exception getError() {
        return err;
    }
    public void setError(Exception ex) {
        err = ex;
    }
    public int getState() {
        if (!prepFlag) prepSolution();
        return state;
    }
    public boolean isSinchronizable() {
        boolean retval = false;
        if (!prepFlag) prepSolution();
        retval = isFullAccessToChild(srcFObj) && tgtDir.canWrite();
        return retval;
    }
    private boolean isFullAccessToChild(File fileObj) {
        boolean retval = fileObj.canRead();
        if (fileObj.isDirectory()) {
            for (File f : fileObj.listFiles()) {
                retval = isFullAccessToChild(f);
                if (!retval) break;
            }
        }
        return retval;
    }
    @Override
    public String toString() {
        return (fileSRC!=null?fileSRC.toString():fileTGT.toString());
    }
    @Override
    public int compareTo(Object t) {
        return this.toString().compareTo(t.toString());
    }
}
