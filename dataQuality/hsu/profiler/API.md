# **Profiler API Specification**


#### - 요청 메시지 명세

<table>
<thead>
<th colspan=4>항목명(영문)</th><th>항목명(국문)</th><th>항목 <br />구분</th><th>샘플데이터</th><th>항목설명</th><th>2차년도<br />지원여부</th>
</thead>
<tbody>
<tr><td colspan=4>source</td><td>객체 경로</td><td>1</td><td>-</td><td>프로파일링 대상 객체 정보 (local file path, URL, database connection 중 하나)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=3>type</td><td>객체 타입</td><td>1</td><td>path, url, db</td><td>path: 로컬 파일 경로 <br />url: 원격파일 경로에 대한 URL <br />db: database 테이블 connection 정보<br /><br />위 3가지 타입(type, url, db)에 따라 아래 항목(path, url, db) 중 하나를 반드시 사용 (one & only one)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=3>path</td><td>파일 경로</td><td>0</td><td>C:\QUFA\data\sample.csv</td><td>프로파일링 대상 파일의 로컬 경로</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=3>url</td><td>원격 파일경로에 대한 URL</td><td>0</td><td>http://qufa.com/sample.csv</td><td>프로파일링 대상 파일의 URL 경로</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=3>db</td><td>데이터베이스 및 테이블 연결 정보</td><td>0</td><td>-</td><td>프로파일링 대상이 데이터베이스 테이블일 경우 연결 및 테이블 정보<br />* 'db'와 하위 파라미터는 2022년 지원 예정</td><td></td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=2>database</td><td>DBMS명종류</td><td>0</td><td>-</td><td>DBMS 이름</td><td></td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=2>conn</td><td>connection string</td><td>0</td><td>-</td><td>DB 연결을 위한 connection string</td><td></td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=2>table</td><td>테이블명</td><td>0</td><td>-</td><td>프로파일링을 위한 테이블 이름</td><td></td></tr>
<tr><td colspan=4>header</td><td>헤더</td><td>0</td><td>true, false</td><td>csv 파일 헤더에 컬럼 이름의 존재 여부를 나타내는 boolean 값(default:true)<br />* 'header' 파라미터는 2022년 지원 예정</td><td></td></tr>
<tr><td colspan=4>profiles</td><td>컬럼별 프로파일링 항목 정의</td><td>0</td><td>-</td><td>프로파일 종류별 프로파일링 대상이 되는 컬럼들을 컬럼번호나 컬럼이름 리스트로 표현. 생략될 경우 데이터 타입에 따라 가능한 프로파일링을 수행하며, 데이터 타입은 서버에서 자동으로 추정하여 실행함<br />(default:<br />　{<br />　　basic:[모든컬럼리스트],<br />　　number:[수치형 컬럼리스트],<br />　　string:[문자열 컬럼리스트],<br />　　date:[날짜형 컬럼리스트]<br />　}<br />)
<br />* 'profiles'와 하위 파라미터는 2022년 지원 예정</td><td></td></tr>
<tr><td colspan=1></td><td colspan=3>basic</td><td>기본 프로파일링 대상 컬럼 리스트</td><td>0..n</td><td>-</td><td>기본 프로파일링 대상되는 컬럼 리스트 (컬럼번호 또는 컬럼명)</td><td></td></tr>
<tr><td colspan=1></td><td colspan=3>number</td><td>수치 프로파일링 대상 컬럼 리스트</td><td>0..n</td><td>-</td><td>수치 프로파일링 대상되는 컬럼 리스트 (컬럼번호 또는 컬럼명)</td><td></td></tr>
<tr><td colspan=1></td><td colspan=3>string</td><td>문자열 프로파일링 대상 컬럼 리스트</td><td>0..n</td><td>-</td><td>문자열 프로파일링 대상되는 컬럼 리스트 (컬럼번호 또는 컬럼명)</td><td></td></tr>
<tr><td colspan=1></td><td colspan=3>date</td><td>날짜 프로파일링 대상 컬럼 리스트</td><td>0..n</td><td>-</td><td>날짜 프로파일링 대상되는 컬럼 리스트 (컬럼번호 또는 컬럼명)</td><td></td></tr>
</tbody></table>

###### ※ 항목구분 : 필수(1), 옵션(0), 1건 이상 복수건(1..n), 0건 또는 복수건(0..n)
 <br /> <br />

#### - 응답 메시지 명세

<table>
<thead>
<tr><th colspan=5>항목명</th><th>항목<br />구분</th><th width=125>샘플데이터</th><th>항목설명</th><th>2차년도<br />지원여부</th></tr>
</thead>
<tbody>     
<tr><td colspan=5>dataset_name</td><td>0</td><td>sample</td><td>데이터 셋 이름(String)</td><td>O</td></tr>
<tr><td colspan=5>dataset_type</td><td>0</td><td>csv</td><td>데이터 셋 타입(String)</td><td>O</td></tr>
<tr><td colspan=5>dataset_size</td><td>0</td><td>300000</td><td>데이터 셋 크기(byte)</td><td>O</td></tr>
<tr><td colspan=5>dataset_column_cnt</td><td>0</td><td>1</td><td>데이터 셋 열 수(int)</td><td>O</td></tr>
<tr><td colspan=5>dataset_row_cnt</td><td>0</td><td>200</td><td>데이터 셋 행 수(int)</td><td>O</td></tr>
<tr><td colspan=5>results</td><td>0</td><td>-</td><td>profiling 결과</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=4>column_id</td><td>1</td><td>1</td><td>열 고유 번호(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=4>column_name</td><td>1</td><td>score</td><td>열 고유 이름(string)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=4>column_type</td><td>1</td><td>number</td><td>열 타입(string)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=4>profiles</td><td>1</td><td>-</td><td>프로파일 결과 목록</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=3>basic_profile</td><td>0</td><td>-</td><td>basic 프로파일</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>row_cnt</td><td>0</td><td>20</td><td>레코드 수(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>distinct_cnt</td><td>0</td><td>120</td><td>서로 구별되는 값들의 수(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>null_cnt</td><td>0</td><td>0</td><td>결측치 수(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>distinctness</td><td>0</td><td>0.6</td><td>Distinct Count/Row Count(0~1) (double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>unique_cnt</td><td>0</td><td>10</td><td>유일한 값을 갖는 레코드 수(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>value_distribution</td><td>0</td><td>-</td><td>값 분포 정보</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=1>type　　　</td><td>0</td><td>all, top100</td><td>- Value Distribution(VD) 응답 형식<br />- distinct count가 100 초과일 경우 type = top100<br />- distinct count가 100 이하일 경우 type = all</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=1>value　　　</td><td>0..n</td><td>{<br />　"45":10<br />},<br />{<br />　"12":9<br />},<br />{<br />　"10":8<br />},<br />··· ,<br />{<br />　"150":1<br />},<br />{<br />　"149":1<br />}</td><td>- 각 값의 출현 빈도(빈도수 기준 내림차순)<br />- type = all일 경우 모든 분포값을 출력<br />- type = top100일 경우 상위 100개 분포값을 출력
</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=1>range　　　</td><td>0..n</td><td>{<br />　"5":19<br />},<br />{<br />　"19.5":31<br />},<br />··· ,<br />{<br />　"121":23<br />},<br />{<br />　"135.5":12<br />},<br />{<br />　"150":null<br />}</td><td>- 모든 value에 대해 값의 범위 별로 출력<br />- column_type=number, type=top100일 경우에만 출력<br />- {"5":19}, {"19.5":31}일 경우 5보다 크거나 같고 19.5보다 작은 값의 수는 19개임을 나타냄<br />- range는 항상 10개의 구간으로 설정<br />- 마지막 구간의 시작과 끝을 모두 명시하기 위해 <br />11번째 key pair를 {"max number":null}로 설정</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=3>number_profile</td><td>0</td><td>-</td><td>number 프로파일</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>min</td><td>0</td><td>5</td><td>최소값(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>max</td><td>0</td><td>150</td><td>최대값(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>sum</td><td>0</td><td>18512</td><td>총합(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>mean</td><td>0</td><td>92.56</td><td>평균(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>median</td><td>0</td><td>78</td><td>중앙값(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>sd</td><td>0</td><td>21.44</td><td>표준편차(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>variance</td><td>0</td><td>459.674</td><td>분산(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>percentile_25th</td><td>0</td><td>42</td><td>1사분위 수(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>percentile_75th</td><td>0</td><td>113</td><td>3사분위 수(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>zero_cnt</td><td>0</td><td>2</td><td>0의 개수(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=3>string_profile</td><td>0</td><td>-</td><td>string 프로파일</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>blank_cnt</td><td>0</td><td>2</td><td>빈칸의 수(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>min_len</td><td>0</td><td>4</td><td>최소 길이(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>max_len</td><td>0</td><td>9</td><td>최대 길이(int)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>avg_len</td><td>0</td><td>6.2</td><td>평균 길이(double)</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=3>date_profile</td><td>0</td><td>-</td><td>date 프로파일</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>highest_date</td><td>0</td><td>2021-07-01<br />12:00:00</td><td>가장 최신 시간</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>lowest_date</td><td>0</td><td>2020-07-01<br />12:00:00</td><td>가장 과거 시간</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>mean_date</td><td>0</td><td>2021-01-01<br />12:00:00</td><td>평균 시간</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>median_date</td><td>0</td><td>2021-01-01<br />12:00:00</td><td>중앙값을 갖는 시간</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>percentile_25th</td><td>0</td><td>2020-10-01<br />12:00:00</td><td>1사분위 시간</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>percentile_75th</td><td>0</td><td>2021-04-01<br />12:00:00</td><td>3사분위 시간</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>month_distribution</td><td>0</td><td>{<br />　January:7<br />},<br />{<br />　February:8<br />},<br />···,<br />{<br />　December:5<br />}</td><td>월별 분포</td><td>O</td></tr>
<tr><td colspan=1></td><td colspan=1></td><td colspan=1></td><td colspan=2>year_distribution</td><td>0</td><td>{<br />　2010:20<br />},<br />{<br />　2011:10<br />}, ···</td><td>연도별 분포</td><td>O</td></tr>
</tbody></table>

###### ※ 항목구분 : 필수(1), 옵션(0), 1건 이상 복수건(1..n), 0건 또는 복수건(0..n)
 <br /> <br />

---

* **URL**

  /profile/local

* **Method:**

  `POST`
  
*  **JSON Request Params**

   **Required:**
 
   `source:type=[string]`

   **Optional:**
 
   `source:path=[string]` <br />
   `source:url=[string]` <br />
   `header=[boolean]` <br />
   `profiles:basic=[integer array]` <br />
   `profiles:number=[integer array]` <br />
   `profiles:string=[integer array]` <br />
   `profiles:date=[integer array]` <br />

* **Data Params**

  - if: type = path <br />
    ```
    {
        "source": {
          "type": "path",
          "path": "C:\QUFA\data\sample.csv"
        },
        "header": true,
        "profiles: {
          "basic": [1, 2, 3, 4, 5, 6],
          "number": [1, 2],
          "string": [3, 4],
          "date": [5, 6],
        }
    }
    ```

  - if: type = "url" <br />
    ```
    {
        "source": {
          "type": "url",
          "url": "http://qufa.com/sample.csv"
        }
        //param 'header', 'profiles' can use like above
    }`
    ```

* **Success Response:**

  * **Code:** 200 OK <br />
    **Content:** <br />
    ```
    {
        "dataset_name": "sample",
        "dataset_type": "csv",
        "dataset_size": 30000,
        "dataset_column_cnt": 1,
        "dataset_row_cnt": 200,
        "results": [
          {
            "column_id": 1,
            "column_name": "score",
            "column_type": "number",
            "profiles": {
              "basic_profile": {
                "row_cnt": 200,
                "distinct_cnt": 120,
                "null_cnt": 0,
                "distinctness": 0.6,
                "unique_cnt": 10,
                "value_distribution": {
                  "type": "top100",
                  "value": [
                    {
                      "45": 10
                    },
                    {
                      "12": 9
                    },
                    {
                      "10": 8
                    },
                    ...,
                    {
                      "150": 1
                    },
                    {
                      "149": 1
                    }
                  ],
                  "range": [
                    {
                      "5": 19
                    },
                    {
                      "19.5": 31
                    },
                    {
                      "34": 15
                    },
                    {
                      "48.5": 27
                    },
                    {
                      "63": 18
                    },
                    {
                      "77.5": 20
                    },
                    {
                      "92": 22
                    },
                    {
                      "106.5": 23
                    },
                    {
                      "121": 23
                    },
                    {
                      "135.5": 12
                    },
                    {
                      "150": null
                    }
                  ]
                }
              },
              "number_profile": {
                "min": 5,
                "max": 150,
                "sum": 18512,
                "mean": 92.56,
                "median": 78,
                "sd": 21.44,
                "variance": 459.674,
                "percentile_25th": 42,
                "percentile_75th": 113,
                "zero_cnt": 0
              }
            }
          }
        ]
    }
    ```
  <br />

* **Error Response:**

  * **Code:** 400 Bad Json Request <br />
    **Content:** `{ status_code : "BAD_JSON_REQUEST" }`

  OR

  * **Code:** 411 Request Data Malformed <br />
    **Content:** `{ status_code : "REQUEST_DATA_MALFORMED" }`

  OR

  * **Code:** 500 Internal Error <br />
    **Content:** `{ status_code : "INTERNAL_ERROR" }`
  <br />

* **Notes:**

  - Swagger Hub Link : https://app.swaggerhub.com/apis/jee101kr/QuFa/1.0.0
