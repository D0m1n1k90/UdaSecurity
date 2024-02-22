module security {
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    requires image;
    requires java.desktop;
    requires miglayout.swing;
    opens com.udacity.catpoint.security.data to com.google.gson;
}