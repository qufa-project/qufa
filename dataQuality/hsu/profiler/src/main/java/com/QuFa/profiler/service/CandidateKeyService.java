package com.QuFa.profiler.service;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class CandidateKeyService {
    private boolean key_analysis; // 후보키를 찾을지 말지 판단
    private ArrayList<Object> key_analysis_results; // 후보키 컬럼을 담는 배열
    private int nullCnt = 0;
    public CandidateKeyService() {
        key_analysis_results = new ArrayList<>();
    }
    public void CheckCandidateKey(double distinctness, int id) {
        /* key analysis result */
        /* 후보키 : 널값이 존재하지 않고  Distinct Count/Row Count 가 1인 경우*/
        if (nullCnt == 0 && distinctness == 1) {
                key_analysis_results.add(id);
        }
    }
    public void addNullCnt(int blankCnt) {
        nullCnt += blankCnt;
    }

}
