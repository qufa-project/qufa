[ Fairness indicator ]
- "2020_qufa_fairness_indicator_v0_1.ipynb" 실행 환경: <a href=https://colab.research.google.com>Google Colaboratory</a>
- <a href="https://colab.research.google.com/github/google/eng-edu/blob/master/ml/cc/exercises/intro_to_ml_fairness.ipynb?utm_source=mlcc&utm_campaign=colab-external&utm_medium=referral&utm_content=fairness-colab&hl=ko#scrollTo=J8daw3YOIAXH">Source</a>: Google Colaboratory, "intro_to_fairness.ipynb"
1. 균등 기회 (Equal Opportunity)
definition: 보호 그룹과 보호되지 않은 그룹은 동일한 참긍정(True Positive)의 비율을 가져야 함
damage field 에서 미화 1,500 달러 이하와 미화 1,500 달러 초과 확인을 위한 Prediction
Category weather_condition 중 Subgroup CLEAR 입력에 따른 TPR과 Subgroup RAIN 입력에 따른 TPR 이 같아야만 균등 기회(Equal Opportunity)를 만족
2. 균등 승률 (Equalized odds)
definition: 보호된 그룹과 보호되지 않은 그룹은 참긍정(True Positive)과 오탐지(False Negative)에 대해 동일한 비율을 가져야 함
damage field 에서 미화 1,500 달러 이하와 미화 1,500 달러 초과 확인을 위한 Prediction
Category weather_condition 중 Subgroup CLEAR 입력에 따른 TPR, FNR 과 Subgroup RAIN 입력에 따른 TPR, FNR 이 같아야만 균등 승률(Equalized odds)을 만족

- cf: https://developers.google.com/machine-learning/glossary/fairness