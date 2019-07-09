/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synchronizingdirectories;

/**
 *
 * @author Icemen
 */
public interface SinchListener {
    public void progressed (double percent);
    public void progresstotal (double percent);
    public void prepared(String text, boolean important);
    public void started();
    public void reportready(String text, boolean important);
}
