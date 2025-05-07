package com.git.amarradi.leafpad;

import android.app.Application;

import java.io.IOException;

public class Leafpad extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialisierung hier
    }

   public void close() throws IOException {
        // Alle notwendigen Ressourcen freigeben
    }
}
