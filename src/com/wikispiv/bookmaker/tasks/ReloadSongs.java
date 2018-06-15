package com.wikispiv.bookmaker.tasks;

import java.io.IOException;

import javax.swing.SwingWorker;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.ui.BookMakerFrame;

public class ReloadSongs extends SwingWorker<Void, Void>
{
    private BookMakerFrame bmf;
    private Main main;

    public ReloadSongs(BookMakerFrame bmf)
    {
        this.bmf = bmf;
        this.main = this.bmf.getMain();
        this.bmf.doingSomething(true);
    }

    @Override
    public Void doInBackground()
    {
        try {
            this.main.reloadSongs(this);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public void setMyProgress(int n)
    {
        super.setProgress(n);
    }

    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done()
    {
        this.bmf.doingSomething(false);
    }
}