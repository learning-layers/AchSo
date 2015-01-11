package fi.aalto.legroup.achso.util;

import android.net.Uri;

import java.util.List;

/**
 * Created by lassi on 8.12.14.
 */
public class ExportCreatorTaskResultEvent {
    private List<Uri> result;

    public ExportCreatorTaskResultEvent(List<Uri> result) {
        this.result = result;
    }

    public List<Uri> getResult() {
        return this.result;
    }
}
