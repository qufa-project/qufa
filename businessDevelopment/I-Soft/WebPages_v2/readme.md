# QuFa Visualization (2차)

1. 서버 기반 웹 서비스
2. 보정 전/후 Dataset을 OverView 테이블, 차트, 다이어그램 및 선버스트 차트로 가시화
3. Logistic Regression 예측 모델 사용 예측 수치 계산 및 알고리즘 API 로 전달
4. 보정 전/후 예측 수치의 Confusion Matrix 히트맵 가시화 및 UMAP & tSNE 맵으로 가시화
5. 공정성 지표 선택 및 보정률 표시 및 개선율에 대하여 차트로 가시화
6. 각각의 공정성 지표에 대한 수식 표시

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