module com.udacity.catpoint.security {
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires miglayout.swing;
    opens com.udacity.catpoint.security.data to com.google.gson;
}