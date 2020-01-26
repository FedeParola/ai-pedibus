#############
### build ###
#############

# base image
FROM node:10.16.3 as build

# set working directory
WORKDIR /app

# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH

# add app
COPY . /app

# install and cache app dependencies
RUN npm install --unsafe-perm
RUN npm install -g @angular/cli@8.3.8

# generate build
RUN ng build --prod --output-path=dist

############
### prod ###
############

# base image
FROM nginx:1.16.0-alpine

# copy artifact build from the 'build environment'
COPY --from=build /app/dist /usr/share/nginx/html

# copy nginx configuration file
COPY default.conf /etc/nginx/conf.d/default.conf

# expose port 80
EXPOSE 80

# run nginx
CMD ["nginx", "-g", "daemon off;"]
