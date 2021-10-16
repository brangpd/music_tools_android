package io.brangpd.ui.midipiano;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import org.billthefarmer.mididriver.MidiDriver;

import java.util.Locale;

import io.brangpd.R;
import io.brangpd.databinding.MidiPianoFragmentBinding;

public class MidiPianoFragment extends Fragment {
    private static final String TAG = MidiPianoFragment.class.toString();

    private MidiPianoViewModel mViewModel;
    private int mWhiteKeyWidth = 100;
    private int mWhiteKeyHeight = 450;
    private TextView mTextViewMidiNote;
    private Spinner mSpinnerMidiKeySize;
    private ConstraintLayout mConstraintLayoutMidiKeyboard;
    private MidiPianoFragmentBinding mBinding;
    private HorizontalScrollView mScrollView;

    public static MidiPianoFragment newInstance() {
        return new MidiPianoFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MidiDriver.getInstance().start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MidiDriver.getInstance().stop();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(MidiPianoViewModel.class);
        View view = inflater.inflate(R.layout.midi_piano_fragment, container, false);
        mBinding = MidiPianoFragmentBinding.bind(view);
        mScrollView = mBinding.scrollViewMidiKeyboard;
        mConstraintLayoutMidiKeyboard = mBinding.constraintLayoutMidiKeyboard;
        mConstraintLayoutMidiKeyboard.requestDisallowInterceptTouchEvent(true);
        mTextViewMidiNote = mBinding.textViewMidiNote;
        mSpinnerMidiKeySize = mBinding.spinnerMidiKeySize;
        mSpinnerMidiKeySize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mWhiteKeyWidth = Integer.parseInt(adapterView.getSelectedItem().toString());
                buildKeys();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        buildKeys();

        return view;
    }

    private void buildKeys() {
        mConstraintLayoutMidiKeyboard.removeAllViews();
        ConstraintLayout.LayoutParams params;

        // 底部添加一点空隙方便拖拽
        Space space = new Space(getContext());
        params = new ConstraintLayout.LayoutParams(52 * mWhiteKeyWidth, mWhiteKeyHeight + 100);
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        space.setLayoutParams(params);
        mConstraintLayoutMidiKeyboard.addView(space);

        OnMidiKeyboardListener onMidiKeyboardListener = new OnMidiKeyboardListener() {
            @Override
            public void onTouch(int midiCode, int velocity) {
                Log.d(TAG, String.format("onCreateView Touch: %d %d", midiCode, velocity));
                mTextViewMidiNote.setText(midiString(midiCode));
                // Construct a note ON message for the middle C at maximum velocity on channel 1:
                byte[] event = new byte[3];
                event[0] = (byte) 0x90;  // 0x90 = note On, 0x00 = channel 1
                event[1] = (byte) midiCode;  // 0x3C = middle C
                event[2] = (byte) velocity;  // 0x7F = the maximum velocity (127)

                // Send the MIDI event to the synthesizer.
                MidiDriver.getInstance().write(event);
            }

            @Override
            public void onRelease(int midiCode, int velocity) {
                Log.d(TAG, String.format("onCreateView Release: %d", midiCode));
                // Construct a note OFF message for the middle C at minimum velocity on channel 1:
                byte[] event = new byte[3];
                event[0] = (byte) 0x80;  // 0x80 = note Off, 0x00 = channel 1
                event[1] = (byte) midiCode;  // 0x3C = middle C
                event[2] = (byte) velocity;  // 0x00 = the minimum velocity (0)

                // Send the MIDI event to the synthesizer.
                MidiDriver.getInstance().write(event);
            }
        };
        // 钢琴第一个键A0，左边省去20个MIDI键，即12个白键
        int leftMarginOffset = -12 * mWhiteKeyWidth;
        L_octave_loop:
        for (int octave = 0; ; ++octave) {
            for (int i = 0; i < 12; ++i) {
                int midiCode = octave * 12 + i;
                if (midiCode < 21 /*A0*/) {
                    continue;
                }
                if (midiCode > 108 /*C8*/) {
                    break L_octave_loop;
                }
                MidiKeyboardButton midiKeyboardButton = new MidiKeyboardButton(
                        new ContextThemeWrapper(
                                getContext(), R.style.Widget_MaterialComponents_Button_OutlinedButton),
                        midiCode);
                midiKeyboardButton.setOnMidiKeyboardListener(onMidiKeyboardListener);
                if (isBlackKey(midiCode)) {
                    // 放置黑键
                    midiKeyboardButton.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
                    params = new ConstraintLayout.LayoutParams(
                            blackKeyWidth(mWhiteKeyWidth), blackKeyHeight(mWhiteKeyHeight));
                    params.topMargin = 0;

                    params.leftMargin = leftMarginOffset + octave * 7 * mWhiteKeyWidth + i * blackKeyWidth(mWhiteKeyWidth) + 1;
                    Log.d(TAG, String.format("Black key %d left margin: %d", midiCode, params.leftMargin));
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                    midiKeyboardButton.setLayoutParams(params);
                } else {
                    // 放置白键
                    midiKeyboardButton.setStrokeColor(ColorStateList.valueOf(Color.BLACK));

                    params = new ConstraintLayout.LayoutParams(mWhiteKeyWidth, mWhiteKeyHeight);
                    params.topMargin = 0;

                    // 算当前八度内白键为第几个
                    int localWhiteIndex = i / 2;
                    if (i >= 5) {
                        ++localWhiteIndex;
                    }

                    params.leftMargin = leftMarginOffset + octave * 7 * mWhiteKeyWidth + localWhiteIndex * mWhiteKeyWidth;
                    Log.d(TAG, String.format("White key %d left margin: %d", midiCode, params.leftMargin));
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                    midiKeyboardButton.setLayoutParams(params);
                }
                mConstraintLayoutMidiKeyboard.addView(midiKeyboardButton);
            }
        }
    }

    private static int blackKeyWidth(int whiteKeyWidth) {
        return whiteKeyWidth * 7 / 12;
    }

    private static int blackKeyHeight(int whiteKeyHeight) {
        return whiteKeyHeight * 3 / 5;
    }

    private static boolean isBlackKey(int midiCode) {
        switch (midiInOctave(midiCode)) {
            case 1:
            case 3:
            case 6:
            case 8:
            case 10:
                return true;
            default:
                return false;
        }
    }

    private static int midiInOctave(int midiCode) {
        return midiCode % 12;
    }

    private static int midiOfOctave(int midiCode) {
        return midiCode / 12 - 1;
    }

    private static String midiString(int midiCode) {
        if (isBlackKey(midiCode)) {
            int leftWhite = midiCode - 1;
            int rightWhite = midiCode + 1;
            return String.format(Locale.getDefault(), "#%s / b%s", midiString(leftWhite), midiString(rightWhite));
        }
        char name; // CDEFGAB
        switch (midiInOctave(midiCode)) {
            case 0:
                name = 'C';
                break;
            case 2:
                name = 'D';
                break;
            case 4:
                name = 'E';
                break;
            case 5:
                name = 'F';
                break;
            case 7:
                name = 'G';
                break;
            case 9:
                name = 'A';
                break;
            case 11:
                name = 'B';
                break;
            default:
                return null;
        }
        return String.format(Locale.getDefault(), "%c%d", name, midiOfOctave(midiCode));
    }

    private interface OnMidiKeyboardListener {
        void onTouch(int midiCode, int velocity);

        void onRelease(int midiCode, int velocity);
    }

    private class MidiKeyboardButton extends MaterialButton {
        private final int kMidiCode;
        private OnMidiKeyboardListener mListener = null;

        public MidiKeyboardButton(Context context, int midiCode) {
            super(context);
            this.kMidiCode = midiCode;
            int backgroundColor = isBlackKey(midiCode) ? Color.BLACK : Color.WHITE;
            setBackgroundColor(backgroundColor);
            setRippleColor(ColorStateList.valueOf(Color.GRAY));
            setElevation(isBlackKey(midiCode) ? 1 : 0);
            setStateListAnimator(null);
        }

        public void setOnMidiKeyboardListener(OnMidiKeyboardListener e) {
            mListener = e;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            performClick();
            Log.d(TAG, String.format("onTouchEvent: %s %d", midiString(kMidiCode), event.getAction()));
            switch (event.getAction()) {
                default:
                    return false;
                case MotionEvent.ACTION_DOWN:
                    if (mListener != null) {
                        mListener.onTouch(kMidiCode, (int) (event.getPressure() * 127));
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mListener != null) {
                        mListener.onRelease(kMidiCode, 0);
                    }
                    return true;
            }
        }
    }
}