## 공정성


> 목표

모델 데이터에서 나타날 수 있는 여러 유형의 편향에 대한 이해도 제고모델을 학습시키기 전에 특성 데이터를 살펴보고 잠재적 데이터 편향 요인을 미리 파악종합집계하는 대신 하위 그룹으로 묶어 모델 성능을 평가


> 개요

머신러닝(ML)에 원치 않는 편향이 발생할 수 있는 방식에 주목하면서 공정성을 염두에 두고 데이터세트를 살펴보고 분류자를 평가공정성에 관한 ML 프로세스의 컨텍스트를 구성할 기회를 제공하는 Fairness 작업을 확인. 작업을 진행하는 동안 편향을 파악하고, 이러한 편향이 해결되지 않을 때 발생하는 모델 예측의 장기적인 영향을 고려 데이터세트 및 Prediction 작업 정보


> 보건 데이터셋을 활용

원본 데이터세트에서 ML 공정성에 영향을 미칠 수 있는 field만을 임의 선택하여 학습에 사용Binary(이진) Features

**( 출처: NIA(한국지능정보사회진흥원) 빅데이터 플랫폼 및 센터 구축 사업 中 "암예방-진단 라이프스타일 데이터 센터"의 데이터 연계, http://cancerpreventionsnu.kr/ )**

sex: 성별 / cva: 뇌졸중 과거력 / fcvayn: 뇌졸중 가족력

_Numeric(수적) Features_ - packyear: 하루 흡연량(갑) X 흡연기간 / packyear: 일주일간 음주 빈도 / exerfq: 일주일간 운동한 총 일수

_Categorical(범주적) Features_ - age: 나이

_Prediction 작업_ - 예측 작업은 조사 대상자의 성별을 예측하기 위해 실행Label / sex: 조사 대상자의 성별을 나타냄

> 공정성 지표

**1. 균등 기회 (Equal Opportunity)**

definition: 보호 그룹과 보호되지 않은 그룹은 동일한 참긍정(True Positive)의 비율을 가져야 함sex field 에서 남녀 성별 확인을 위한 PredictionCategory cva 중 Subgroup 0(뇌졸중 과거력 있음) 입력에 따른 TPR과 Subgroup 1(뇌졸중 과거력 없음) 입력에 따른 TPR이 같아야만 균등 기회(Equal Opportunity)를 만족

**2. 균등 승률 (Equalized odds)**

definition: 보호된 그룹과 보호되지 않은 그룹은 참긍정(True Positive)과 오탐지(False Negative)에 대해 동일한 비율을 가져야 함sex field 에서 남녀 성별 확인을 위한 PredictionCategory cva 중 Subgroup 0(뇌졸중 과거력 있음) 입력에 따른 TPR, FPR과 Subgroup 1(뇌졸중 과거력 없음) 입력에 따른 TPR, FPR이 같아야만 균등 승률(Equalized odds)을 만족

**3. 인구통계패리티 (Demographic Parity)**

definition: 긍정적인 결과의 가능성은 개인이 보호된(예 : 여성) 그룹에 있는지 여부 에 관계없이 동일해야 함sex field 에서 남녀 성별 확인을 위한 PredictionCategory cva 중 Subgroup 0(뇌졸중 과거력 있음) 입력에 따른 TP+FP/TN+FN과 Subgroup 1(뇌졸중 과거력 없음) 입력에 따른 TP+FP/TN+FN이 같아야만 인구통계패리티 (Demographic Parity)을 만족

> 요약

**batch-algorithm**

in-memory Spark 기반 실시간 배치 알고리즘

**data-fairness-algorithm**

performance measures 기반의 데이터 공정성 알고리즘

**data-fairness-assessment**

데이터셋의 보정 전/후 비교에 따른 공정성 보정률 검증 모델

**data-fairness-integration**

데이터셋 공정성 보정 알고리즘 + 보정률 검증 모델 통합

**data-fairness-visualize**

웹 기반의 고정성 보정 알고리즘 및 가시화
