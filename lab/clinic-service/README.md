Лабораторна робота №4. AOP (Aspect Oriented Programming)
Мета

Винести наскрізну функціональність (cross-cutting concern) з бізнес-логіки в аспекти, які підключаються декларативно через власні анотації. Замість копіювання коду (вимірювання часу, аудит, обмеження частоти викликів) у кожен метод, достатньо поставити одну анотацію над методом.

Що реалізовано

Створено 3 власні анотації та 3 відповідні аспекти з різними типами advice:

#	Анотація	Аспект	Тип advice	Наскрізна функціональність
1	@MeasureTime	MeasureTimeAspect	@Around	Вимірювання часу виконання методу. Якщо час перевищує поріг (warnThresholdMs), у лог пишеться WARN
2	@Audited	AuditAspect	@AfterReturning	Журнал аудиту: хто (ID користувача з токена), яку дію (action) і з якими аргументами виконав. Запис з'являється лише якщо метод завершився без винятку
3	@RateLimited	RateLimitAspect	@Around	Обмеження частоти викликів на одного користувача (maxCalls викликів за windowSeconds секунд). Аспект сам вирішує: викликати proceed() чи кинути RateLimitExceededException (HTTP 429)

Усі анотації мають @Retention(RUNTIME) (щоб бути видимими під час роботи програми) і @Target(METHOD). Аспекти знаходять методи через pointcut @annotation(...), який також дає доступ до параметрів самої анотації (поріг, назва дії, ліміт).

Де застосовано у робочому коді

Анотації поставлені в AppointmentServiceImpl:

Метод	Анотація	Навіщо
findAllVisibleToCurrentUser()	@MeasureTime	відстежувати швидкість вибірки записів
create(...)	@RateLimited(maxCalls = 5, windowSeconds = 60)	захист від масового створення записів на прийом одним користувачем
cancel(...)	@Audited(action = "CANCEL_APPOINTMENT")	журнал скасувань прийомів
delete(...)	@Audited(action = "DELETE_APPOINTMENT")	журнал видалень записів
Структура доданого коду
src/main/java/com/clinic/
├── aop/
│   ├── annotation/   MeasureTime, Audited, RateLimited
│   ├── aspect/       MeasureTimeAspect, AuditAspect, RateLimitAspect
│   └── demo/         SelfInvocationDemoService, AopDemoController
├── config/           AopConfig (бін Clock)
└── exception/        RateLimitExceededException

src/test/java/com/clinic/aop/
├── aspect/           MeasureTimeAspectTest, AuditAspectTest, RateLimitAspectTest
└── demo/             SelfInvocationTest

Також у pom.xml додано залежність spring-boot-starter-aop.

Як працюють аспекти
MeasureTimeAspect. Запам'ятовує час перед joinPoint.proceed() і після нього (у блоці finally, тому час фіксується навіть якщо метод кинув виняток). Залежно від порогу пише INFO або WARN.
AuditAspect. Спрацьовує після успішного завершення методу. Бере ID поточного користувача через SecurityUtils (працює і для JWT, і для OIDC-сесії) та пише в лог запис [AUDIT] action=... user=... method=... args=.... Якщо метод кинув виняток (наприклад, AccessDeniedException), запису немає, бо дія фактично не виконалась.
RateLimitAspect. Для кожної пари «користувач + метод» зберігає час останніх викликів. Виклики, старші за вікно, відкидаються. Якщо у вікні вже maxCalls викликів, кидається RateLimitExceededException і proceed() не викликається, тобто сам метод не виконується.

Час у MeasureTimeAspect і RateLimitAspect береться з біна Clock, а не з системного часу. Завдяки цьому тести детерміновані: у них підставляється фейковий годинник.

Демонстрація проблеми самовиклику (self-invocation)

Клас SelfInvocationDemoService містить метод timedOperation() з анотацією @MeasureTime та метод callViaThis(), який викликає його через this.timedOperation().

Шлях виклику	Чи йде через проксі	Результат
Ззовні: demoService.timedOperation() (з контролера чи тесту)	так	аспект спрацьовує, у лозі є рядок [MeasureTime] ... took ... ms
Зсередини класу: this.timedOperation()	ні, this це реальний об'єкт, а не проксі	аспект не спрацьовує, без жодної помилки чи попередження

Це свідомий задум вправи, а не помилка. Spring AOP працює через проксі-об'єкти, а виклик this.method() обходить проксі.

Як побачити наживо (потрібен JWT, ендпоінти під /api/**):

GET /api/demo/aop/external у консолі з'являється запис [MeasureTime];
GET /api/demo/aop/self відповідь така сама, але запису [MeasureTime] немає.

Автоматично це перевіряє SelfInvocationTest на мінімальному Spring-контексті (лише аспект і демо-сервіс, без БД та Keycloak).

Тести
Тест	Що перевіряє
MeasureTimeAspectTest	швидкий метод: proceed() викликано, результат повернуто, лог INFO; повільний метод: лог WARN; метод кинув виняток: виняток прокидається далі, час усе одно залоговано
RateLimitAspectTest	виклики в межах ліміту проходять; перевищення ліміту кидає RateLimitExceededException, а proceed() не викликається; різні користувачі мають незалежні ліміти; після закінчення вікна виклики знову дозволені
AuditAspectTest	запис містить дію, ID користувача, метод та аргументи; для неавтентифікованого користувача пишеться anonymous
SelfInvocationTest	бін обгорнутий у проксі; зовнішній виклик активує аспект; self-invocation не активує

Тести аспектів є чистими unit-тестами: ProceedingJoinPoint (та JoinPoint) замоканий через Mockito, без Spring-контексту і без @SpringBootTest. Перевіряється саме логіка рішень усередині advice (чи викликається proceed(), що відбувається, якщо ні). Перевірка запису в лог зроблена через ListAppender (Logback).

Відповідність сценаріям із завдання
Сценарій	Де виконано
Власна анотація (RUNTIME, @Target) пов'язана з аспектом через @annotation(...)	MeasureTime/Audited/RateLimited та pointcut-и в *Aspect
Наскрізна поведінка спрацьовує (лог, виняток)	лог у MeasureTimeAspect та AuditAspect, виняток у RateLimitAspect
Зовнішній виклик активує аспект	SelfInvocationTest.externalCall_goesThroughProxy_aspectRuns, ендпоінт /api/demo/aop/external
Self-invocation не активує аспект	SelfInvocationTest.selfInvocation_viaThis_bypassesProxy_aspectDoesNotRun, ендпоінт /api/demo/aop/self
Advice перевіряється unit-тестом з mock ProceedingJoinPoint без Spring	MeasureTimeAspectTest, RateLimitAspectTest (AuditAspectTest з mock JoinPoint)