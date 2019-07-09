/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
    Класс инкапсулитует пару сравниваемых файловых объектов устанавливая
    алгоритм их синхронизации в соответствии с перечислимой константой getType()
    класса SynchFolders. Данный алгоритм описывается перечислимой константой
    this.getState().
*/
package synchronizingdirectories;

import java.io.File;
import java.util.Date;

/**
 *
 * @author icemen78
 */
public class FileEntry  implements Comparable {
    public static final int FE_UNKNOWN = 0;         //тип не установлен
    public static final int FE_SINGLE = 1;          //одиночный файловый объект
    public static final int FE_FOLDERS = 2;         //две папки
    public static final int FE_FILES_EQUAL = 3;     //одинаковые файлы
    public static final int FE_FILES_NOEQUAL = 4;   //различные файлы
    public static final int FE_FILE_AND_FOLDER = 5; //одноименные файл и папка
    
    private final File fileSRC, fileTGT;
    private final Date ds, dt;
    private final SynchFolders parent;
    
    private int state = FE_UNKNOWN;
    
    private File fileNewer=null;
    private File fileMirror=null;
    
    private String sortPrefix="";
    private long reqSRC=0;
    private long reqTGT=0;
    
    public FileEntry(File s, File t, SynchFolders sf) {
        fileSRC = s;
        if (s==null) ds=null; else ds = new Date(s.lastModified());
        fileTGT = t;            
        if (t==null) dt=null; else dt = new Date(t.lastModified());
        parent = sf;
        if ((fileSRC!=null) && (fileTGT!=null) && (fileSRC.isFile()) && (fileTGT.isFile())) {
            if (ds.equals(dt)) {
                state = FileEntry.FE_FILES_EQUAL;
            }else {
                state = FileEntry.FE_FILES_NOEQUAL;
                fileNewer=ds.compareTo(dt)>0?fileSRC:fileTGT;
                fileMirror=fileNewer.equals(fileSRC)?fileTGT:fileSRC;
            }
        }else if ((fileSRC!=null) && (fileTGT!=null) && (fileSRC.isFile()!=fileTGT.isFile())) {
            state = FileEntry.FE_FILE_AND_FOLDER;
        }else if (fileSRC!=null && fileTGT!=null && fileSRC.isDirectory() && fileTGT.isDirectory()) {
            state = FileEntry.FE_FOLDERS;
        }else if (fileSRC==null ^ fileTGT==null) {
            state = FileEntry.FE_SINGLE;
            String strSRCroot=parent.src.getAbsolutePath();
            String strTGTroot=parent.tgt.getAbsolutePath();
            String strNewer;
            if (fileTGT==null) {
                fileNewer=fileSRC;
                strNewer=fileNewer.toString();
                fileMirror= new File(strTGTroot+strNewer.substring(strSRCroot.length()));
            }else {
                fileNewer=fileTGT;
                strNewer=fileNewer.toString();
                fileMirror= new File(strSRCroot+strNewer.substring(strTGTroot.length()));
            }
        }
        if (fileNewer==null) {fileNewer=fileSRC;}
        if (fileMirror==null) {fileMirror=fileTGT;}
    }
    
    public long getSize(File file) {
        long retval=0;
        if (file!=null) {
            if (file.isFile()){
                retval=file.length();
            }else {
                for (File f : file.listFiles()){retval+=getSize(f);}
            }
        }
        return retval;
    }
    public long getMaxFSize(File file) {
        long retval=0;
        if (file!=null) {
            if (file.isFile()){
                retval=file.length();
            }else {
                for (File f : file.listFiles()){
                    long l = getSize(f);
                    retval = retval<l?l:retval;
                }
            }
        }
        return retval;
    }
    
    public File getSourceFObj() {
        return fileSRC;
    }
    public File getTargetFObj() {
        return fileTGT;
    }
    public File getNewerFObj() {
        return fileNewer;
    }
    public File getMirrorFObj() {
        return fileMirror;
    }
    public int getState() {
        return state;
    }
    
            
    public boolean isReadAccessToChild(File fileObj) {
        boolean retval = fileObj.canRead();
        if (retval && fileObj.isDirectory()) {
            for (File f : fileObj.listFiles()) {
                retval = isReadAccessToChild(f);
                if (!retval) break;
            }
        }
        return retval;
    }
    public boolean isWriteAccessToChild(File fileObj) {
        boolean retval = fileObj.canWrite();
        if (retval && fileObj.isDirectory()) {
            for (File f : fileObj.listFiles()) {
                retval = isWriteAccessToChild(f);
                if (!retval) break;
            }
        }
        return retval;
    }
    
    public String getSortPrefix() {
        return sortPrefix;
    }
    public void setSortPrefix(String sortPref) {
        sortPrefix = sortPref;
    }
    public long getSRC_req() {
        return reqSRC;
    }
    public void setSRC_req(long value) {
        reqSRC=value;
    }
    public long getTGT_req() {
        return reqTGT;
    }
    public void setTGT_req(long value) {
        reqTGT=value;
    }
    @Override
    public String toString() {
        return (fileSRC!=null?sortPrefix+fileSRC.toString():sortPrefix+fileMirror.toString());
    }
    @Override
    public int compareTo(Object t) {
        return this.toString().compareTo(t.toString());
    }
}
