module br.edu.unex.sentinela {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires java.desktop;

    exports br.edu.unex.sentinela.audio;
    exports br.edu.unex.sentinela.bestiary;
    exports br.edu.unex.sentinela.progress;
    exports br.edu.unex.sentinela.settings;
    exports br.edu.unex.sentinela.ui;

    exports br.edu.unex.sentinela.ai;
    exports br.edu.unex.sentinela.app;
    exports br.edu.unex.sentinela.core;
    exports br.edu.unex.sentinela.entity;
    exports br.edu.unex.sentinela.input;
    exports br.edu.unex.sentinela.rendering;
    exports br.edu.unex.sentinela.world;
}
