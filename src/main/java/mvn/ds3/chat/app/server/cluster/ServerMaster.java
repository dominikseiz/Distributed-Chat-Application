package mvn.ds3.chat.app.server.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMaster {
    private final AtomicBoolean master;

    public ServerMaster() {
        this.master = new AtomicBoolean(true);
    }

    public boolean isMaster() {
        return master.get();
    }

    public void setIsMaster(boolean master) {
        this.master.set(master);
    }
}
