package io.brangpd.ui.midipiano;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MidiPianoViewModel extends ViewModel {
    private final MutableLiveData<Integer> transposition = new MutableLiveData<>(0);

    public LiveData<Integer> getTransposition() {
        return transposition;
    }

    public void setTransposition(int delta) {
        this.transposition.setValue(delta);
    }
}