# lossfit (loss data fitting tool)

- lossfit은 실수값 기반의 tabular data에 대해 결측 데이터값을 자동보정하는 도구
- 현재 지원되는 입력 데이터는 CSV 형식의 서울시 상수도 수질 측정 데이터

## 실행 환경

- python3, scikit-learn 필요

## Tools

- getloss: 결측율 측정, 기존 원본 데이터와 비교한 결측율 제공 가능
   ```
  getloss.py [-e <max error>] <path> [<original>]
     -e <max error>: maximum error rate to original value(0-1)
  ```
  - original(원본) 경로가 제공되는 경우, 해당 원본값과의 오류 허용범위를 초과하는 경우도 결측으로 판단
  - -e 옵션의 최대 허용값은 0에서 1사이로서 1의 경우는 원본값의 100%까지 허용
- makegood: 결측 데이터를 제외한 정상 데이터 저장 도구
  `makegood.py <path> <result path>` 
- makeloss: 데이터에 결측 데이터를 랜덤하게 추가
  `makeloss.py [-r <lossrate>] <path> <result path>`
  - -r 옵션의 lossrate는 0에서 1사이. 예로 0.1은 10% 데이터를 결측값으로 변경
- lossfit: 결측데이터 보정
  `lossfit.py <path> <result path>`

## 실행예제
```
# ./getloss.py sample.csv
Loss rate: 6.3855% (854/13374)
# ./makegood.py sample.csv sample.good.csv
# ./makeloss.py -r 0.05 sample.good.csv sample.loss.csv
# ./getloss.py sample.loss.csv
Loss rate: 5.0% (626/12520)
# ./lossfit.py sample.loss.csv sample.fit.csv
# ./getloss.py sample.fit.csv sample.good.csv
Loss rate: 2.524% (316/12520)
#
```
