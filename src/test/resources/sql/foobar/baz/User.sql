SELECT
    *
FROM
    users
---: allOrdered
ORDER BY name
---: getById
WHERE id=:userId
---: matchByName
WHERE name LIKE :name