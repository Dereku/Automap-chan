
import java.util.Comparator;

public class Sample {

    private long startTime;
    private String hitSound;
    private int volume;

    public Sample(long t, String hs, int v) {
        startTime = t;
        hitSound = hs;
        volume = v;
    }

    public void addQuotesToHS() {
        hitSound = "\"" + hitSound + "\"";
    }

    @Override
    public String toString() {
        return "Sample," + startTime + ",0," + hitSound + "," + volume;
    }

    public boolean equals(Sample s) {
        return startTime == s.startTime && hitSound.equals(s.hitSound) && volume == s.volume;

    }

    @Override
    public Sample clone() {
        Sample s = new Sample(startTime, hitSound, volume);
        return s;
    }

    /* For ascending order */
    public static Comparator<Sample> StartTimeComparator = (Sample n1, Sample n2) -> (int) (n1.startTime - n2.startTime);
}
