package com.demo.ntc;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdRecognizer;
import com.microblink.results.date.DateResult;
import com.microblink.image.Image;

public class BlinkIdModal extends BottomSheetDialogFragment {

    Button nextBtn;
    ImageButton closeBtn;
    ImageView scanFace;
    TextView crnTv, fullnameTv, genderTv, bdateTv, addressTv;
    private ModalListener modalListener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_main_modal, container, false);

        BlinkIdRecognizer.Result result = getArguments().getParcelable("data");

        crnTv = v.findViewById(R.id.crnTv);
        fullnameTv = v.findViewById(R.id.fullnameTv);
        genderTv = v.findViewById(R.id.genderTv);
        bdateTv = v.findViewById(R.id.bdateTv);
        addressTv = v.findViewById(R.id.addressTv);
        scanFace = v.findViewById(R.id.scanFace);

        crnTv.setText("CRN: " + result.getDocumentNumber());
        String fullname = result.getFirstName() + " " + result.getLastName();
        fullnameTv.setText("FULLNAME: " + fullname.replaceAll("[\n\r]", " "));
        genderTv.setText("GENDER: " + result.getSex());
        DateResult dateResult = result.getDateOfBirth();
        bdateTv.setText("BIRTHDATE: " + dateResult.getOriginalDateString());
        addressTv.setText("ADDRESS: " + result.getAddress());
        Image image =result.getFaceImage();
        if(image != null){
            scanFace.setImageBitmap(image.convertToBitmap());
        }

        closeBtn = v.findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modalListener.onClick(v);
                dismiss();
            }
        });

        nextBtn = v.findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modalListener.onClick(v);
                dismiss();
            }
        });

        return v;
    }

    public interface ModalListener{
        void onClick(View view);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            modalListener = (ModalListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "must implement ModalListener");
        }
    }
}
