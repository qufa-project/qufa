# Data Quality Enhancer (v2.0)
- 데이터 품질 평가기반 데이터 고도화 및 데이터셋 보정 기술 개발
- python 3.7.6
- 사용법: `DataQualityEnhancer.ipynb`

## 소프트웨어 구성
|이름|용도|비고|
|---|---|---|
|`get_numerical_missing_rate.py`|수치데이터 결측률 측정||
|`data_imputation.py`|수치데이터 결측률 개선|Deep Neural Network 기반 missing value imputer|
|`get_categorical_missing_rate.py`|범주데이터 결측률 측정|
|`data_imputation.py`|범주데이터 결측률 개선|Deep Neural Network 기반 missing value imputer|
|`get_outlier_rate.py`|이상치 비율 측정|
|`outlier_detection.py`|이상치 비율 개선|AutoEncoder 기반 outlier detector|
|`get_text_error_rate.py`|텍스트 오류율 측정|
|`text_correction.py`|텍스트 오류율 개선|`py-hanspell` 기반 텍스트 오류 개선|
|`format_validation.py`|포맷 오류율 측정|
|`format_validation.py`|포맷 오류율 개선|정규 표현식 기반 포맷 오류 개선|

# 실험 데이터 구성
- `./data`
    - `arisu.csv`: 서울시 상수도 수질 데이터
    - `perform_info.csv`: 예술경영지원센터 공연예술통합전산망(KOPIS) 제공
공연상세정보 데이터
    - `box_office.csv`: 영화진흥위원회 영화관입장권 통합전산망(KOBIS) 제공
일별 박스오피스 데이터
    - `recbook.csv`: 한국출판문화산업진흥원의 추천도서 청소년권장도서 데이터
    - `event.csv`: 공공데이터포털 제공 전국공연행사정보표준데이터 데이터
#
