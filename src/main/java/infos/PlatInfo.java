package infos;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/19 10:41
 */
public class PlatInfo {
    private final String gameName;
    private final String platName;


    public String getPlatName() {
        return platName;
    }

    public int getPlatId() {
        return platId;
    }

    public void setPlatId(int platId) {
        this.platId = platId;
    }

    private int platId;

    public PlatInfo(String gameName, String platName) {
        this.gameName = gameName;
        this.platName = platName;
    }
}
