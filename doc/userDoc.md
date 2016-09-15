# FTPDownloader. Руководство пользователя.

Запуск:  
1) перейти в каталог проекта  
2) выполнить ./start.sh  
Загрузчик запустится в режиме демона, потоки вывода будут перенаправлены в output.txt, обычно этот файл содержит мало информации или не содержит её вовсе. PID процесса будет сохранен в файле pid.txt

Останов:  
1) перейти в каталог проекта  
2) выполнить ./stop.sh  
При этом загрузчику будет послан сигнал SIGTERM (остановится). Загрузчик остановится не мгновенно, а лишь тогда, когда текущий загружаемый файл будет загружен до конца. В логах должно появится сообщение:  
=======FTPDownloader stopped===========

# Файлы конфигурации.  
Конфигурируется загрузчик при помощи нескольких xml-файлов, расположенных в каталоге config

```xml
1)ftpConfig.xml - этот файл содержит реквизиты доступа к фтп-серверу,
с которого нужно логи забирать либо на который нужно выгружать файлы.
<FTPConfig>
	<host>10.230.16.34</host>
	<port>21</port>
	<username>getcdr</username>
  <password>getcdr</password>
</FTPConfig>

2)jobConfig.xml - конфигурация работы фтп-загрузчика
jobType = DOWNLOAD|UPLOAD - тип работы, загрузка или выгрузка
cronSchedule - крон-расписание, по которому будет запускаться работа.
<jobConfig>
   <ftpInitialDir>/usr/protei/data/Protei-DPI/cdr/gy_cdr_test</ftpInitialDir>
   <destFolderPath>ftpFiles</destFolderPath>
   <filePattern>^AS.*$</filePattern>
   <files>3</files>
   <cronSchedule>0 * * * * ?</cronSchedule>
   <deleteSourceFile>false</deleteSourceFile>
	 <jobType>DOWNLOAD</jobType>
</jobConfig>
3)processedFiles.txt - это файл, содержащий список обработанных имен файлов.
4)logger.xml - стандартный файл конфигурации logback
```

Логи располагаются в каталоге logs.
