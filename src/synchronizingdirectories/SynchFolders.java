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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
//import java.nio.file.Files;
//import java.nio.file.attribute.PosixFilePermissions;
import javax.swing.JOptionPane;

/**
 *
 * @author icemen78
 */
public class SynchFolders implements Runnable{
    //только копирование отсутствующих директорий/файлов с заменой старых файлов новыми
    //т.е. мы не знаем какая директория важнее
    public static final int SF_AGREGATE = 0;
    //безусловное копирование(в т.ч. удаление) файлов в target из source
    //т.е. приведение целевой папки к состоянию контрольной
    public static final int SF_CONTROL_FOLDER = 1;
    //безусловное копирование(удаление) из более свежей папки в устаревшую
    //т.е. актуализация осуществляется с использованием истории/логов
    public static final int SF_RELATIVE = 2;
    
    
    public static final String DELETE_PREFIX = "<delete>";
    
    public static final boolean SF_CHECK_HIDDEN_DEFAULT = false;
    
    private List<SinchListener> listeners = new ArrayList<>();
    
    public final File src;
    public final File tgt;
    private final int type;
    private Date srcDate, tgtDate;              //Даты каталогов из файла
    private final String fileInf = "_sync";
    
    private TreeSet<FileEntry> files;
    private TreeSet<ErrorEntry> ferrors;
    private BufferedInputStream rbuffSource, rbuffTarget;
    private BufferedOutputStream wbuffSource, wbuffTarget;
    
    private static final int BUFFER_SIZE = 8388608;
    private Date sTime = new Date();            //время старта программы
    private Date fTime = new Date();            //время завершения программы
    
    private long totalsize=0;
    private long currentsize=0;
    long maxSRCfilelength=0;
    long maxTGTfilelength=0;
    private volatile boolean interrupted;
    private boolean checkHiddenFO = false;
    
    public SynchFolders(File dirSource, File dirTarget, int typeSinch) throws Exception {

        if ((!dirSource.exists() || !dirTarget.exists()) || (dirSource.isFile() || dirTarget.isFile())) {
            throw new FileNotFoundException();
        }
        
        src = dirSource;
        tgt = dirTarget;
        type = typeSinch;
        
        File sinf, tinf;    //файлы синхронизации
        String skey, tkey;  //ключи синхронизации (наименование параиетра файлового архива)
        Date sdate, tdate;  //дата синхронизации

        sdate = null;
        tdate = null;
            
        switch (this.getType()) {
            case SynchFolders.SF_RELATIVE:
                //реализация сценария протоколируемой синхронизации
                //... в разработке
                //<...>
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
                break;
        }
        srcDate = sdate;
        tgtDate = tdate;
        files = new TreeSet<>();
        ferrors = new TreeSet<>();
        totalsize=0;
        currentsize=0;
    }
    
    public void interrupt() {
        interrupted=true;
    }
    
    @Override
    public void run() {
        files.clear();
        ferrors.clear();
        boolean important=false;
        boolean breakprocess=false;
        String prepreport="";
        try {
            //Подготовка...
            files = preparing(src,tgt);
            
            totalsize=getTotalSize();
            
            long srs_available;
            long tgt_available;
            long single_available;
            String err = "";
            if (src.getFreeSpace()==tgt.getFreeSpace()) {
                //Каталоги в одном разделе
                single_available=src.getFreeSpace()-this.getSRC_req()-this.getTGT_req()-(maxSRCfilelength>maxTGTfilelength?maxSRCfilelength:maxTGTfilelength);
                if (single_available<0) {err +=(err.length()==0?"\n":"")+ ("Недостаточно места в разделе: " + Math.abs(single_available) + " bites \n");}
            }else {
                srs_available=src.getFreeSpace()-this.getSRC_req()-maxSRCfilelength;
                tgt_available=tgt.getFreeSpace()-this.getTGT_req()-maxTGTfilelength;
                if (srs_available<0) {
                    err += "\n";
                    err += ("Недостаточно места в первичном местоположении: " + Math.abs(srs_available) + " bites \n");
                }
                if (tgt_available<0) {
                    err += ("Недостаточно места во вторичном местоположении: " + Math.abs(tgt_available) + " bites \n");
                }                
            }
            if (err.length()>0) {
                throw new Exception("<NoPrintStackTrace>"+err);
            }
            
            
            prepreport="Подготовка закончена. Файловых объектов для синхронизации: "+files.size()+"\n";
            if (ferrors.size()>0) {
                important=true;
                prepreport += "Ошибок найдено: "+ferrors.size()+"\n";
                int i=0;
                for (ErrorEntry ee : ferrors) {
                    i++;
                    prepreport +=i + ": " + ee.toString()+ "\n";
                    TreeMap<String,Exception> tm = ee.getDetails();
                    for (String key:ee.getDetails().keySet()) {
                        prepreport += "|          " + key+ "\n";
                        prepreport += "|          " + tm.get(key).toString()+ "\n";
                    }
                }
            }
            fTime = new Date();
            prepreport += "Время выполнения (сек): " + ((fTime.getTime()-sTime.getTime())/1000)+ "\n"; 
            this.prepared(prepreport, important);
            if (files.size()>0) {
                if (important) {
                    breakprocess = JOptionPane.showConfirmDialog(null, "Выявлены ошибки. Продолжить выполнение?","Предупреждение", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)!=JOptionPane.OK_OPTION;
                }else {
                    breakprocess = JOptionPane.showConfirmDialog(null, "Ошибок не найдено. Нажниме ОК для продолжения","Подтветждение", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION;
                    if (breakprocess) {throw new Exception("<NoPrintStackTrace>"+"Отменено пользователем");}
                }
                //Синхронизация...
                if (!breakprocess) {
                    sTime=fTime;
                    this.started();
                    ferrors = synchronizing();
                }
            }
            this.updateStoredInf(null);   
        }catch (Exception e) {
            this.updateStoredInf(getErrorStack(e));
        } 
    }
    
    public void synch() {
        Thread t = new Thread(this, "sinch");
        t.start();
    }
    public void addListener(SinchListener obj) {
        listeners.add(obj);
    }
    public void removeListener(SinchListener obj) {
        listeners.remove(obj);
    }
    
    private TreeSet<FileEntry> preparing(File src, File tgt) throws Exception {
        TreeSet<FileEntry> retval = new TreeSet<>();
        
        TreeMap<String,File> slf = new TreeMap<>();
        TreeMap<String,File> tlf = new TreeMap<>();
        
        TreeMap<String,Exception> exeptionContainer = new TreeMap<>();
        try {
            for (File f : src.listFiles()) {if (!(checkHiddenFO && f.isHidden())) {slf.put(f.getName(),f);}}
        }catch (NullPointerException npe) {
            exeptionContainer.put(src.toString(), new Exception("Отсутстует доступ к папке"));
        }
        try {
            for (File f : tgt.listFiles()) {if (!(checkHiddenFO && f.isHidden())) {tlf.put(f.getName(),f);}} 
        }catch (NullPointerException npe) {
            exeptionContainer.put(tgt.toString(), new Exception("Отсутстует доступ к папке"));
        }
        if (exeptionContainer.size()>0) {
            ErrorEntry ee = new ErrorEntry(new FileEntry(src, tgt, this),exeptionContainer);
            ferrors.add(ee);
            return retval;           
        }

        //Просматриваем пару каталогов, поэтому можем использовать короткое имя файла для сравнения
        TreeSet<String> srclist = new TreeSet<>(slf.keySet());
        TreeSet<String> tgtlist = new TreeSet<>(tlf.keySet());

        tgtlist.removeAll(srclist);
        srclist.addAll(tgtlist);
        
        if (this.src.equals(src) && !slf.isEmpty()) {
            int d,t;
            t=slf.size();
            d=tgtlist.size();
            if ((double)d/t>0.5) {
                //больше половины файлов уникальны, запрос подтветждения
                if (JOptionPane.showConfirmDialog(null, "Директории сильно отличаются, продолжить?","Информация", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) {
                    throw new Exception("<NoPrintStackTrace>"+"Отменено пользователем");
                }
            }
        }
        
        Iterator iterator = srclist.iterator();
        while (iterator.hasNext()) {
            
            if (interrupted) {throw new Exception("<NoPrintStackTrace>"+"Прервано пользователем");}
            
            String key = iterator.next().toString();
            FileEntry fe = new FileEntry((File)slf.get(key),(File)tlf.get(key), this);
            switch (this.getType()) {
                case SynchFolders.SF_AGREGATE:
                    switch (fe.getState()){
                        case FileEntry.FE_FOLDERS:
                            //разбираем пару папок. РЕКУРСИЯ. проверка на чтение обоих папок
                            if (requestAccess(fe,false,false,false)) {
                                retval.addAll(preparing(fe.getSourceFObj(), fe.getTargetFObj()));
                            }
                            break;
                        case FileEntry.FE_FILE_AND_FOLDER:
                            //Генерируем исключение ErrorEntry, т.к. в этом сценарии синхронизация не поддерживается
                            TreeMap<String,Exception> tm = new TreeMap<>();
                            tm.put(fe.getSourceFObj().toString(), new Exception("Одноименные файл и папка"));
                            ErrorEntry ee = new ErrorEntry(fe,tm);
                            ferrors.add(ee);
                            break;
                        case FileEntry.FE_FILES_NOEQUAL:
                            //проверка на чтение свежий файл и на запись старый файл
                            if (requestAccess(fe,false,true,false)) {
                                long lsrc=fe.getSize(fe.getSourceFObj());
                                long ltgt=fe.getSize(fe.getTargetFObj());
                                if (fe.getNewerFObj().equals(fe.getSourceFObj())) {
                                    fe.setTGT_req(lsrc-ltgt);
                                    maxTGTfilelength = maxTGTfilelength<lsrc?lsrc:maxTGTfilelength;
                                }else {
                                    fe.setSRC_req(ltgt-lsrc);
                                    maxSRCfilelength = maxSRCfilelength<ltgt?ltgt:maxSRCfilelength;
                                }
                                retval.add(fe);
                            }
                            break;
                        case FileEntry.FE_FILES_EQUAL:
                            //действий не требуется, т.к. пара эквивалентна
                            break;
                        case FileEntry.FE_SINGLE:
                            //добавляем одиночные файловые объекты
                            //проверка на чтение свежего файлового объекта с содержимым и на запись в новое местоположение
                            if (requestAccess(fe,false,true,true)) {
                                long l = 0;
                                if (fe.getNewerFObj().equals(fe.getSourceFObj())) {
                                    l = fe.getSize(fe.getSourceFObj());
                                    fe.setTGT_req(l);
                                    l=fe.getMaxFSize(fe.getSourceFObj());
                                    maxTGTfilelength = maxTGTfilelength<l?l:maxTGTfilelength;
                                }else {
                                    l = fe.getSize(fe.getTargetFObj());
                                    fe.setSRC_req(l);
                                    l=fe.getMaxFSize(fe.getTargetFObj());
                                    maxSRCfilelength = maxSRCfilelength<l?l:maxSRCfilelength;
                                }
                                retval.add(fe);
                            }
                            break;
                        default:
                    }
                    break;
                case SynchFolders.SF_CONTROL_FOLDER:
                    switch (fe.getState()){
                        case FileEntry.FE_FOLDERS:
                            //разбираем пару папок. РЕКУРСИЯ. проверка на чтение обоих папок
                            if (requestAccess(fe,false,false,false)) {
                                retval.addAll(preparing(fe.getSourceFObj(), fe.getTargetFObj()));
                            }
                            break;
                        case FileEntry.FE_FILE_AND_FOLDER:
                            //проверка на чтение файловый объект источника и на запись заменяемый файловый объект
                            if (requestAccess(fe,false,false,false)) {
                                fe.setSortPrefix(SynchFolders.DELETE_PREFIX);
                                long lsrc=fe.getSize(fe.getSourceFObj());
                                long ltgt=fe.getSize(fe.getTargetFObj());
                                fe.setTGT_req(lsrc-ltgt);
                                if (fe.getSourceFObj().isFile()) {
                                    maxTGTfilelength = maxTGTfilelength<lsrc?lsrc:maxTGTfilelength;
                                }else {
                                    long l = fe.getMaxFSize(fe.getSourceFObj());
                                    maxTGTfilelength = maxTGTfilelength<l?l:maxTGTfilelength;
                                }
                                retval.add(fe);
                            }
                            break;
                        case FileEntry.FE_FILES_NOEQUAL:
                            //проверка на чтение файл источника и на запись заменяемый файл
                            if (requestAccess(fe,false,true,false)) {
                                long lsrc=fe.getSize(fe.getSourceFObj());
                                long ltgt=fe.getSize(fe.getTargetFObj());
                                fe.setTGT_req(lsrc-ltgt);
                                maxTGTfilelength = maxTGTfilelength<lsrc?lsrc:maxTGTfilelength;
                                retval.add(fe);
                            }
                            break;
                        case FileEntry.FE_FILES_EQUAL:
                            //действий не требуется, т.к. пара эквивалентна
                            break;
                        case FileEntry.FE_SINGLE:
                            //добавляем одиночные файловые объекты
                            //проверка на чтение файловый объект источника (с содержимым) и на запись новое местоположение
                            if (requestAccess(fe,false,true,true)) {
                                long l = 0;
                                if (fe.getNewerFObj().equals(fe.getSourceFObj())) {
                                    l = fe.getSize(fe.getSourceFObj());
                                    fe.setTGT_req(l);
                                    l = fe.getMaxFSize(fe.getSourceFObj());
                                    maxTGTfilelength = maxTGTfilelength<l?l:maxTGTfilelength;
                                } else {
                                    fe.setSortPrefix(SynchFolders.DELETE_PREFIX);
                                    l = fe.getSize(fe.getTargetFObj());
                                    fe.setTGT_req((-1)*l);
                                }
                                retval.add(fe);
                            }
                            break;
                        default:
                    }
                    break;
                case SynchFolders.SF_RELATIVE:
                    //Данный сценарий в разработке...
                    if (!key.equals("_sync")) {
                        //<...>
                    }
                    break;
                default:
            }
        }
        return retval;
    }
    
    private boolean requestAccess(FileEntry fileentry, boolean waNewverFObject, boolean waMirrorFObject, boolean includingChild) {
        boolean retval;
        TreeMap<String,Exception> tm = new TreeMap<>();
        tm.putAll(requestAccess(fileentry.getNewerFObj(),waNewverFObject,includingChild));
        tm.putAll(requestAccess(fileentry.getMirrorFObj(),waMirrorFObject,includingChild));
        retval = (tm.size()==0);
        if (!retval) {ferrors.add(new ErrorEntry(fileentry,tm));}
        return retval; 
    }
    
    private TreeMap<String,Exception> requestAccess(File fileobject, boolean waFObject, boolean includingChild) {
        TreeMap<String,Exception> retval = new TreeMap<>();
        if (includingChild & fileobject.isDirectory()) {
            for (File f:fileobject.listFiles()) {
                retval.putAll(requestAccess(f,waFObject,includingChild));
            }
        } else {
            if (waFObject) {
                if (!fileobject.exists()) {
                    fileobject = fileobject.getParentFile();
                }
                if (!fileobject.canWrite()) {
                    retval.put(fileobject.toString(), new Exception("file write error"));
                }
            }else if (!fileobject.canRead()) {
                retval.put(fileobject.toString(), new Exception("file read error"));
            }
        }
        return retval;
    }
    
    private TreeSet<ErrorEntry> synchronizing() throws Exception{
        TreeSet<ErrorEntry> retval = new TreeSet<>();
        Iterator iterator = files.iterator();
        while (iterator.hasNext()) {
            FileEntry fe = (FileEntry)iterator.next();
            TreeMap<String, Exception> errfiles = new TreeMap<>();
            try {
                switch (this.getType()) {
                    case SynchFolders.SF_AGREGATE:
                        switch (fe.getState()) {
                            case FileEntry.FE_SINGLE:
                                //рекурсивно копируем файловые объекты
                                errfiles.putAll(copyFObject(fe.getNewerFObj(),fe.getMirrorFObj()));
                                break;
                            case FileEntry.FE_FILES_NOEQUAL:
                                //актуализируем пару файлов
                                errfiles.putAll(copyFObject(fe.getNewerFObj(),fe.getMirrorFObj()));
                                break;
                            case FileEntry.FE_FILE_AND_FOLDER:
                                //Проблема одноименного файла и папки
                                //В данном сценарии операция не поддерживается
                                break;
                            default:
                        }
                        break;
                    case SynchFolders.SF_CONTROL_FOLDER:
                        switch (fe.getState()) {
                            case FileEntry.FE_SINGLE:
                                //рекурсивно копируем или удаляем файловые объекты
                                if (fe.getTargetFObj()==null) {
                                    errfiles.putAll(copyFObject(fe.getNewerFObj(),fe.getMirrorFObj()));
                                }else {
                                    errfiles.putAll(deleteFObject(fe.getTargetFObj(),true));
                                }
                                break;
                            case FileEntry.FE_FILES_NOEQUAL:
                                errfiles.putAll(copyFObject(fe.getSourceFObj(),fe.getTargetFObj()));
                                break;
                            case FileEntry.FE_FILE_AND_FOLDER:
                                //приводим в соответствие пару файловых объектов с источником
                                //т.е. удаляем файл или папку в целевой директории и копируем из источника
                                //!!!удаляем без подсчета размера файлового объекта, копипуем с подсчетом
                                errfiles.putAll(deleteFObject(fe.getTargetFObj(),false));
                                errfiles.putAll(copyFObject(fe.getSourceFObj(),fe.getTargetFObj()));
                                break;
                            default:
                        }
                        break;
                    case SynchFolders.SF_RELATIVE:
                        //Данный сценарий в разработке...
                        //<...>
                        break;
                    default:
                }
                if (errfiles.size()>0) {
                    retval.add(new ErrorEntry(fe,errfiles));
                }
                if (interrupted) {throw new Exception("<NoPrintStackTrace>"+"Прервано пользователем");}
            } catch (Exception e) {
//                System.out.println("... была ошибка");
//                retval.add(new ErrorEntry(e,currentCopied,fe));
                throw e;
            }
        }
        return retval;
    }
    private TreeMap<String,Exception> copyFObject(File sobject, File tobject){
        TreeMap<String, Exception> retval = new TreeMap<>();
        
        if (!interrupted) {
            TmpFile tobject_tmp = null;
            if (sobject.isDirectory()) {
                String errorDescription="";

    //            boolean readonly=false;
    //            boolean hidden=false;
    //            readonly=sobject.canRead();
    //            hidden=sobject.isHidden();

                if (tobject.mkdir()) {
                    if (tobject.setLastModified(sobject.lastModified())) {
                        for (File f : sobject.listFiles()) {
                            File childobject = new File(tobject.getAbsolutePath()+File.separator+f.getName());
                            retval.putAll(copyFObject(f,childobject));
                        }
                    }else {
                        errorDescription="change last midified directory error";
                    }
                }else {
                    errorDescription="create directory error";
                }
                if (errorDescription.length()>0) {
                    retval.put(tobject.toString(), new Exception("create directory error"));
                }
            }else {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesCopied;
                try {
                    //<если tobject существует, создаем имя временного файла tobject_tmp>
                    //<...>
                    tobject_tmp = new TmpFile(tobject);
    //                System.out.println("tobject date: "+tobject.getAbsolutePath() + "  "+tobject.lastModified());
    //                System.out.println("sobject date: "+sobject.getAbsolutePath() + "  "+sobject.lastModified());
                    fis = new FileInputStream(sobject);
                    fos = new FileOutputStream(tobject_tmp);
                    long total = sobject.length();
                    long current = (long) 0;
    //                long buflen = (long) BUFFER_SIZE;
                    while ((bytesCopied = fis.read(buffer)) > 0) {
                        if (interrupted) {throw new Exception("<NoPrintStackTrace>"+"Прервано пользователем");}
                        fos.write(buffer, 0, bytesCopied);
                        //Пользовательское событие
                        current += bytesCopied;
                        currentsize +=bytesCopied;                    
                        progressed(new Double(current), new Double(total));
                        totalprogressed(new Double(currentsize), new Double(totalsize));
                    }
                    fis.close();
                    fos.close();
                    //Удаляем файл и переименовываем временный файл
                    //установка аттрибутов
                    tobject_tmp.commit();
                    if (!(tobject.setLastModified(sobject.lastModified())) || tobject.lastModified()!=sobject.lastModified()) {throw new Exception("Не удалось установить дату изменения файла");}
                }catch (Exception e) {
    //                System.out.println(e.getMessage());
                    try {
                        if (fis!=null)fis.close();
                        if (fos!=null)fos.close();
                        //Откатываем изменения
                        tobject_tmp.rollup();
                    } catch (IOException ex) {
                        //Действий не требуется
                    }
                    retval.put(tobject.toString(),e);
                }
            }
        }
        return retval;
    }
    
    //event progressed
    private void progressed(Double posCurrent, Double posTotal) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if (posTotal!=null && posCurrent!=null && posTotal >0 && posCurrent <=posTotal) {
            for (SinchListener sl:listeners) {sl.progressed(posCurrent/posTotal);}
        }
    }
    
    //event totalprogressed
    private void totalprogressed(Double posCurrent, Double posTotal) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if (posTotal!=null && posCurrent!=null && posTotal >0 && posCurrent <=posTotal) {
            for (SinchListener sl:listeners) {sl.progresstotal(posCurrent/posTotal);} 
        }
    }
    
    private TreeMap<String,Exception> deleteFObject(File fobject, boolean updateProgressEvents){
        TreeMap<String, Exception> retval = new TreeMap<>();
        Double filesize=new Double(fobject.length());
        if (fobject.isFile()) {
            if (fobject.delete()) {
                if (updateProgressEvents) {
                    currentsize +=filesize; 
                    progressed(new Double(100), new Double(100));
                    totalprogressed(new Double(currentsize), new Double(totalsize));                    
                }
            }else {
                retval.put(fobject.toString(), new Exception("file delete error"));
            }
        }else {
            for (File f : fobject.listFiles()) {
                retval.putAll(deleteFObject(f,updateProgressEvents));            
            }
            if (!fobject.delete()) {
                retval.put(fobject.toString(), new Exception("file delete error"));
            }
        }
        return retval;
    }
    public long getTotalSize() {
        long retval=0;
        if (totalsize==0) {
            Iterator iterator = files.iterator();
            FileEntry fe;
            while (iterator.hasNext()) {
                fe =(FileEntry)iterator.next();
                retval +=fe.getSize(fe.getNewerFObj());
//                System.out.println(fe.toString());
            }
        } else {
            retval=totalsize;
        }
        return retval;
    }
    public long getSRC_req() {
        long retval=0;
        Iterator iterator = files.iterator();
        FileEntry fe;
        while (iterator.hasNext()) {
            fe =(FileEntry)iterator.next();
            retval +=fe.getSRC_req();
        }
        return retval;
    }
    public long getTGT_req() {
        long retval=0;
        Iterator iterator = files.iterator();
        FileEntry fe;
        while (iterator.hasNext()) {
            fe =(FileEntry)iterator.next();
            retval +=fe.getTGT_req();
        }
        return retval;
    }

    public TreeSet getPreparedList() {
        return files;
    }
    public TreeSet<ErrorEntry> getFileErrorsList() {
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
    public boolean getCheckHidden(){
        return checkHiddenFO;
    }
    public void setCheckHidden(boolean value) {
        checkHiddenFO = value;
    }
    private String getErrorStack(Exception e) {
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
    
    //event prepared
    private void prepared(String description, boolean important) {
        for (SinchListener sl:listeners) {sl.prepared(description,important);}
    }
    //event started
    private void started() {
        for (SinchListener sl:listeners) {sl.started();}
    }
    //event reportready
    private void updateStoredInf(String override) {
        TreeSet<ErrorEntry> fe = this.getFileErrorsList();
        String retval = "";
        if (override==null) {
            retval += "Синхронизируемых объектов: " + this.getPreparedList().size() + "\n";
            retval += "Количество ошибок: " + fe.size()+ "\n";
            int i=0;
            if (fe.size() > 0) {
                for (ErrorEntry ee : fe) {
                    i++;
                    retval +=i + ": " + ee.toString()+ "\n";
                    TreeMap<String,Exception> tm = ee.getDetails();
                    for (String key:ee.getDetails().keySet()) {
                        retval += "|          " + tm.get(key).toString()+ "\n";
                    }
                }
            }
            fTime = new Date();
            retval += "Время выполнения (сек): " + ((fTime.getTime()-sTime.getTime())/1000)+ "\n";           
        }else {
            retval=override;
        }
        for (SinchListener sl:listeners) {sl.reportready(retval, !(override==null && fe.isEmpty()));}
    }
}
