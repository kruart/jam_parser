import os
import re
from multiprocessing import Pool

import requests
from bs4 import BeautifulSoup


def get_html(url) -> str:
    response = requests.get(url)
    return response.text


def get_number_of_pages(html) -> str:
    soup = BeautifulSoup(html, 'lxml')
    href = soup.find_all('div', id='catalog_pager')[-1].find_all('a')[-1]['href']
    # https://jam.ua/picks?list=21  => 21
    index = href.rfind('=')
    num_of_pages = href[index + 1:]
    return num_of_pages


def get_links(html) -> list:
    soup = BeautifulSoup(html, 'lxml')
    divs = soup.find_all('div', id='catalog_item')
    return [div.find('a').get('href') for div in divs]


def get_image_links(html: str) -> dict:
    try:
        domain = 'https://jam.ua'
        images = []

        soup = BeautifulSoup(html, 'lxml')

        title = get_title_item(soup)
        images.append(get_main_image_link(domain, soup))
        if soup.find('div', class_='img-item-gallery') is not None:
            images.extend(get_img_items_gallery(domain, soup))
        data = {title: images}
        print(str(data), 'is parsed')
        return data
    except AttributeError as err:
        print(AttributeError, err)
        return {}


def get_main_image_link(domain: str, soup: BeautifulSoup) -> str:
    beginning_image_link = '/files/'
    parent_div = soup.find('div', id='item_image_wrapper')
    item_img = parent_div.find('div', id='item_img').find('a').find('div')['style']
    start_index = item_img.find(beginning_image_link)
    link = re.split('\'\)', item_img)[0]

    full_link = domain + item_img[start_index:len(link)]
    return full_link


def get_title_item(soup: BeautifulSoup) -> str:
    return soup.find('div', id='item_header_name').find('h1').text.strip()


def get_img_items_gallery(domain: str, soup: BeautifulSoup) -> list:
    divs = soup.find_all('div', class_='img-item-block')
    img_links = [domain + div.find('a')['href'] for div in divs]
    return img_links


def download_images(data: dict):
    parent_folder = './images/'
    for title, links in data.items():
        for link in links:
            print('start working on ', title, link)
            path = parent_folder + title + '/'
            if not os.path.exists(path):
                os.makedirs(path)
            full_path = path + get_img_name(link)

            response = requests.get(link)

            if response.status_code == 200:
                with open(full_path, 'wb') as f:
                    f.write(response.content)

            print(full_path, 'is downloaded.')


def get_img_name(link: str) -> str:
    beginning_name = link.rfind('/')
    return link[beginning_name + 1:]


def make_all(link: str):
    html = get_html(link)
    data = get_image_links(html)

    if data:
        download_images(data)


def main():
    page = 1
    num_of_pages = 0
    links = []

    while True:
        try:
            url = 'https://jam.ua/picks?list={}'.format(str(page))
            html = get_html(url)
            links.extend(get_links(html))

            if num_of_pages == 0:
                num_of_pages = int(get_number_of_pages(html))

            if page < num_of_pages:
                page = page + 1
            else:
                break
        except Exception as e:
            raise Exception(e)

    print('Extract item links is done!')

    # 1. multithreading way
    with Pool(3) as p:
        p.map(make_all, links)

    # 2. single thread way
    # img_links = {}
    # for link in links:
    #     html = get_html(link)
    #     img_links.update(get_image_links(html))
    #
    # print('Extract image links is done!')
    # print('Start downloading... ')
    #
    # download_images(img_links)


if __name__ == '__main__':
    main()
