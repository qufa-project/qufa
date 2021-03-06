# QuFa Visualization (2차)

1. 서버 기반 웹 서비스
2. 보정 전/후 Dataset을 OverView 테이블, 차트, 다이어그램 및 선버스트 차트로 가시화
3. Logistic Regression 예측 모델 사용 예측 수치 계산 및 알고리즘 API 로 전달
4. 보정 전/후 예측 수치의 Confusion Matrix 히트맵 가시화 및 UMAP & tSNE 맵으로 가시화
5. 공정성 지표 선택 및 보정률 표시 및 개선율에 대하여 차트로 가시화
6. 각각의 공정성 지표에 대한 수식 표시

* Overview List/Chart/Diagram, Sunburst Chart :
    - 전체 데이터셋의 개요와 데이터 각각의 특징 면면에 대한 감을 제공
    - 개요에서는 각각의 특징에 대한 통계를 제공하고 훈련 및 검증 데이터셋의 비교 가능
* Confusion Matrix Heatmap Chart : 
    - 혼동행렬(Confusion Matrix)은 분류 모델의 성능을 평가하는 지표로 Heatmap Chart로 가시화
    - 지도학습을 통해 모델링한 "분류 모델이 예측한 값"과 레이블되어 있는 "원래의 값" 간의 관계를 표로 가시화
    - 이 표를 통해 해당 모델의 정확도(accuracy), 정밀도(precision), 민감도(sensitivity), f1 score 등을 파악 가능
    - 특히 정확도를 통해 해당 모델이 정확하게 분류해 낼 수 있는 비율을 확인 가능
* Performance Chart :
    - 데이터 보정 전/후의 지표 차이를 가시화하여 지표 별 성능 개선 정도를 제공
    
## Dataset
* 시카고시 교통사고 관련 데이터 (부산대 제공)
    + traffic_before.csv
* 뇌졸증 발생 관련 데이터 (서울대 제공)
    + health_before.csv

## Development

### 웹 폴더 및 파일 구조
* /root
    + /config
        - settings.py
        - urls.py
    + /Fairness
        + /static
            + /css
                - qufa.css
            + /js
                - qufa.js
                - worker.js
        + /templates
            - index.html
        - alg_tpr.py
        - apps.py
        - urls.py
        - views.py
    + /media
        - health_before.csv
        - health_testset.csv
        - traffic_before.csv
        - traffic_testset.csv

### 개발 환경
* Browser :
    - Google Chrome v96.0
* Language :
    - HTML
    - JAVA SCRIPT
    - Python
    - Django
* JS Library : 
    - Bootstrap
    - D3
    - FontAwesome
    - HighCharts
    - JQuery
    - JQuery.LoadingModal
    - JQuery.ajax-cross-origin
    - MathJax
    - PolyFill
    - Sunburst
* PY Library : 
    - Numpy
    - Matplotlib
    - Pandas
    - Sklearn
    - Tensorflow

## Comment
* 구동 서버 주소 : http://164.125.37.214:8000/Fairness/