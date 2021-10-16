package io.brangpd.ui.metronome;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MetronomeViewModel extends ViewModel {
    private final MutableLiveData<Integer> bpm;
    private final MutableLiveData<Boolean> isPlaying;
    private final MutableLiveData<Long> lastRecordedTimeMs;
    private final MutableLiveData<Integer> recordCount;

    public MetronomeViewModel() {
        bpm = new MutableLiveData<>(60);
        isPlaying = new MutableLiveData<>(false);
        lastRecordedTimeMs = new MutableLiveData<>(0L);
        recordCount = new MutableLiveData<>(0);
    }

    @NonNull
    public LiveData<Integer> getBpm() {
        return bpm;
    }

    public void setBpm(int v) {
        bpm.setValue(v);
    }

    @NonNull
    public LiveData<Boolean> isPlaying() {
        return isPlaying;
    }

    @NonNull
    public LiveData<Long> getLastRecordedTimeMs() {
        return lastRecordedTimeMs;
    }

    public void setLastRecordedTimeMs(long v) {
        lastRecordedTimeMs.setValue(v);
        if (recordCount.getValue() == null) {
            recordCount.setValue(1);
        } else {
            recordCount.setValue(recordCount.getValue() + 1);
        }
    }

    public void resetRecord() {
        recordCount.setValue(0);
        lastRecordedTimeMs.setValue(0L);
    }
}