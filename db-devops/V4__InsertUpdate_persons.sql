UPDATE PERSON SET NAME='Peter Bond' WHERE ID=2;

DROP PROCEDURE IF EXISTS AddPerson;

delimiter //
CREATE PROCEDURE AddPerson (IN myvalue VARCHAR(80))
 BEGIN
   INSERT INTO PERSON (NAME) VALUES (myvalue);
 END //
 
delimiter ;
 
CALL AddPerson('Donald Luck');